package com.yugentech.quill.aira.rag

import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.ChunkVectorTuple
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.text.Normalizer

data class RetrievedChunk(
    val text: String,
    val chapterIndex: Int,
    val chunkIndex: Int,
    val score: Float
)

class RagRetriever(
    private val chunkDao: BookChunkDao,
    private val bookDao: BookDao,
    private val embeddingEngine: EmbeddingEngine
) {

    private var cachedBookId: String? = null
    private var cachedVectors: List<ChunkVectorTuple> = emptyList()
    private val cacheMutex = Mutex()

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
            Timber.e(e, "Error during retrieval for bookId: $bookId")
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

                scored.take(candidatesPerQuery).forEach { (pos, score) ->
                    val existing = mergedMap[pos]
                    if (existing == null || score > existing) {
                        mergedMap[pos] = score
                    }
                }
            }

            if (mergedMap.isEmpty()) return emptyList()
            retrieveAsPassages(bookId, mergedMap.toList(), topPassages)
        } catch (e: Exception) {
            Timber.e(e, "Error during expanded retrieval for bookId: $bookId")
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
            val cleanStr = Normalizer.normalize(query, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace("[^a-zA-Z0-9 ]".toRegex(), " ")

            val words = cleanStr.split("\\s+".toRegex())
                .map { it.lowercase() }
                .filter { it.length > 2 && it !in STOP_WORDS }

            if (words.isEmpty()) {
                emptyList()
            } else {
                val ftsQuery = words.joinToString(" ") { "$it*" }
                try {
                    chunkDao.searchFts(bookId, ftsQuery)
                } catch (e: Exception) {
                    Timber.e(e, "FTS search failed for bookId: $bookId")
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
            try {
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to retrieve neighbor chunks at $pos")
            }
        }

        return expanded.sortedWith(compareBy({ it.chapterIndex }, { it.chunkIndex }))
    }

    private suspend fun getCandidates(
        bookId: String,
        book: BookEntity?,
        spoilerLockEnabled: Boolean
    ): List<ChunkVectorTuple>? {
        return try {
            val allChunks = cacheMutex.withLock {
                if (cachedBookId == bookId && cachedVectors.isNotEmpty()) {
                    cachedVectors
                } else {
                    val fromDb = chunkDao.getCandidateVectors(bookId, Int.MAX_VALUE)
                    cachedBookId = bookId
                    cachedVectors = fromDb
                    fromDb
                }
            }

            if (allChunks.isEmpty()) return null
            if (!spoilerLockEnabled) return allChunks

            val progressCeiling = book?.progressPercent ?: 0f
            if (progressCeiling == 0f) return null

            val maxChapterIndex = (book?.lastChapterIndex ?: 0) + 1
            Timber.d("Spoiler lock active. lastChapterIndex: ${book?.lastChapterIndex}, maxChapterIndex: $maxChapterIndex")

            allChunks.filter { it.chapterIndex <= maxChapterIndex }.ifEmpty { null }

        } catch (e: Exception) {
            Timber.e(e, "Error getting candidates for bookId: $bookId")
            null
        }
    }

    private suspend fun embedQuery(query: String): FloatArray? {
        return try {
            val cleanQuery = Normalizer.normalize(query, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace("\u00AD", "")
                .replace("\u2014", " ")
                .replace("\u2013", " ")
                .trim()

            val queryWithPrefix = "${EmbeddingEngine.BGE_QUERY_PREFIX}$cleanQuery"
            embeddingEngine.embed(queryWithPrefix)
        } catch (e: Exception) {
            Timber.e(e, "Failed to embed query: $query")
            null
        }
    }

    companion object {
        const val DEFAULT_TOP_PASSAGES = 3
        private const val PASSAGE_WINDOW_BEFORE = 1
        private const val PASSAGE_WINDOW_AFTER = 1
        private const val ANCHOR_MIN_SCORE = 0.20f
        private const val RRF_MIN_SCORE = 0.015f

        private val STOP_WORDS = setOf(
            "the", "and", "for", "that", "this", "with", "you", "not", "are", "from",
            "your", "all", "have", "more", "was", "its", "out", "who", "what", "where",
            "when", "why", "how", "has", "but", "into", "his", "her", "she", "him",
            "they", "them", "their", "will", "would", "could", "should", "can", "did", "some"
        )
    }
}