package com.yugentech.quill.aira.rag

import android.util.Log
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.ChunkLocationTuple
import com.yugentech.quill.database.dao.ChunkVectorTuple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RagRetriever(
    private val chunkDao: BookChunkDao,
    private val bookDao: BookDao,
    private val embeddingEngine: EmbeddingEngine
) {

    data class RetrievedChunk(
        val text: String,
        val chapterIndex: Int,
        val chunkIndex: Int,
        val score: Float
    )

    companion object {
        private const val TAG = "QuillRAG"
        const val DEFAULT_TOP_PASSAGES = 3

        private const val PASSAGE_WINDOW_BEFORE = 6
        private const val PASSAGE_WINDOW_AFTER = 3

        private const val ANCHOR_MIN_SCORE = 0.25f
        private const val RRF_MIN_SCORE = 0.015f

        // 🚨 FIX 2: Thread-safe RAM Cache to prevent repeated Room/Cursor allocations
        private var cachedBookId: String? = null
        private var cachedVectors: List<ChunkVectorTuple> = emptyList()
        private val cacheMutex = Mutex()
    }

    suspend fun retrieve(
        bookId: String,
        query: String,
        topPassages: Int = DEFAULT_TOP_PASSAGES,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val candidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()
            val queryEmbedding = embedQuery(query) ?: return emptyList()

            val scored = scoreCandidatesHybrid(bookId, query, candidates, queryEmbedding)
            retrieveAsPassages(bookId, scored, topPassages)
        } catch (e: Exception) {
            Log.e(TAG, "Error in retrieve: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun retrieveWithExpansion(
        bookId: String,
        queries: List<String>,
        topPassages: Int = DEFAULT_TOP_PASSAGES,
        spoilerLockEnabled: Boolean = true,
        candidatesPerQuery: Int = 20
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val candidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()

            val mergedMap = mutableMapOf<Pair<Int, Int>, Float>()

            for (query in queries) {
                val queryEmbedding = embedQuery(query) ?: continue
                val scored = scoreCandidatesHybrid(bookId, query, candidates, queryEmbedding)

                scored.take(candidatesPerQuery)
                    .forEach { (pos, score) ->
                        val existing = mergedMap[pos]
                        if (existing == null || score > existing) {
                            mergedMap[pos] = score
                        }
                    }
            }

            if (mergedMap.isEmpty()) return emptyList()
            retrieveAsPassages(bookId, mergedMap.toList(), topPassages)
        } catch (e: Exception) {
            Log.e(TAG, "Error in retrieveWithExpansion: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun scoreCandidatesHybrid(
        bookId: String,
        query: String,
        candidates: List<ChunkVectorTuple>,
        queryEmbedding: FloatArray
    ): List<Pair<Pair<Int, Int>, Float>> = coroutineScope {

        val vectorSearchDeferred = async(Dispatchers.Default) {
            candidates.mapNotNull { chunk ->
                if (chunk.embedding.size != queryEmbedding.size) return@mapNotNull null
                val sim = EmbeddingEngine.cosineSimilarity(queryEmbedding, chunk.embedding)
                if (sim >= ANCHOR_MIN_SCORE) Pair(chunk, sim) else null
            }.sortedByDescending { it.second }
        }

        val keywordSearchDeferred = async(Dispatchers.IO) {
            val unaccentedQuery = query
                .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

            // 2. NOW we can safely strip out the remaining punctuation
            val cleanStr = unaccentedQuery.replace("[^a-zA-Z0-9 ]".toRegex(), " ")
            val stopWords = setOf(
                "the", "and", "for", "that", "this", "with", "you", "not", "are", "from",
                "your", "all", "have", "more", "was", "its", "out", "who", "what", "where",
                "when", "why", "how", "has", "but", "into", "his", "her", "she", "him",
                "they", "them", "their", "will", "would", "could", "should", "can", "did", "some"
            )

            val words = cleanStr.split("\\s+".toRegex())
                .map { it.lowercase() }
                .filter { it.length > 2 && it !in stopWords }

            if (words.isEmpty()) {
                emptyList<ChunkLocationTuple>() // Explicitly cast to the new Tuple
            } else {
                val ftsQuery = words.joinToString(" ") { "$it*" }
                try {
                    chunkDao.searchFts(bookId, ftsQuery) // Now safely returns the lightweight Tuple
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val vectorRanked = vectorSearchDeferred.await()
        val keywordRanked = keywordSearchDeferred.await()

        val rrfScores = mutableMapOf<Pair<Int, Int>, Float>()
        val k = 60f

        vectorRanked.forEachIndexed { rank, pair ->
            val key = pair.first.chapterIndex to pair.first.chunkIndex
            rrfScores[key] = rrfScores.getOrDefault(key, 0f) + (1f / (k + rank + 1))
        }

        keywordRanked.forEachIndexed { rank, chunk ->
            val key = chunk.chapterIndex to chunk.chunkIndex
            rrfScores[key] = rrfScores.getOrDefault(key, 0f) + ((1f / (k + rank + 1)) * 1.5f)
        }

        rrfScores.toList()
            .filter { it.second >= RRF_MIN_SCORE }
            .sortedByDescending { it.second }
    }

    private suspend fun retrieveAsPassages(
        bookId: String,
        scored: List<Pair<Pair<Int, Int>, Float>>,
        topPassages: Int
    ): List<RetrievedChunk> {
        if (scored.isEmpty()) return emptyList()

        val sortedByScore = scored.sortedByDescending { it.second }
        val usedPositions = mutableSetOf<Pair<Int, Int>>()
        val anchors = mutableListOf<Pair<Pair<Int, Int>, Float>>()

        for ((pos, score) in sortedByScore) {
            if (anchors.size >= topPassages) break
            if (pos in usedPositions) continue

            anchors.add(pos to score)

            for (offset in -PASSAGE_WINDOW_BEFORE..PASSAGE_WINDOW_AFTER) {
                usedPositions.add(pos.first to (pos.second + offset))
            }
        }

        if (anchors.isEmpty()) return emptyList()

        val seen = mutableSetOf<Pair<Int, Int>>()
        val expanded = mutableListOf<RetrievedChunk>()

        for ((pos, score) in anchors) {
            val neighbors = chunkDao.getNeighborChunks(
                bookId = bookId,
                chapterIndex = pos.first,
                fromChunkIndex = pos.second - PASSAGE_WINDOW_BEFORE,
                toChunkIndex = pos.second + PASSAGE_WINDOW_AFTER
            )
            for (chunk in neighbors) {
                val key = chunk.chapterIndex to chunk.chunkIndex
                if (seen.add(key)) {
                    expanded.add(
                        RetrievedChunk(
                            text = chunk.text,
                            chapterIndex = chunk.chapterIndex,
                            chunkIndex = chunk.chunkIndex,
                            score = score
                        )
                    )
                }
            }
        }

        return expanded.sortedWith(compareBy({ it.chapterIndex }, { it.chunkIndex }))
    }

    private suspend fun getCandidates(
        bookId: String,
        book: com.yugentech.quill.database.entity.BookEntity?,
        spoilerLockEnabled: Boolean
    ): List<ChunkVectorTuple>? {

        // 🚨 FETCHING FROM CACHE: The heavy Room parsing is only done ONCE per book
        val allChunks = cacheMutex.withLock {
            if (cachedBookId == bookId && cachedVectors.isNotEmpty()) {
                Log.d(TAG, "  ⚡ Using RAM cached vectors for bookId=$bookId")
                cachedVectors
            } else {
                Log.d(TAG, "  ⏳ Fetching vectors from DB into RAM cache...")
                val fromDb = chunkDao.getCandidateVectors(bookId, Int.MAX_VALUE)
                cachedBookId = bookId
                cachedVectors = fromDb
                fromDb
            }
        }

        if (allChunks.isEmpty()) return null

        if (!spoilerLockEnabled) {
            return allChunks
        }

        val progressCeiling = book?.progressPercent ?: 0f
        if (progressCeiling == 0f) return null

        val totalChapters = allChunks.maxOf { it.chapterIndex } + 1
        val maxChapterIndex = totalChapters.coerceAtLeast(0)

        val filtered = allChunks.filter { it.chapterIndex <= maxChapterIndex }
        return filtered.ifEmpty { null }
    }

    private suspend fun embedQuery(query: String): FloatArray? {
        val cleanQuery = query
            .replace("\u00AD", "")
            .replace("\u2014", " ")
            .replace("\u2013", " ")
            .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .trim()

        val queryWithPrefix = "${EmbeddingEngine.BGE_QUERY_PREFIX}$cleanQuery"
        return embeddingEngine.embed(queryWithPrefix)
    }
}