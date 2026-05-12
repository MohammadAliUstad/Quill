package com.yugentech.quill.aira.rag

import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.ChunkVectorTuple
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.RetrievedChunk
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.text.Normalizer

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

            val scored = scoreVectorOnly(candidates, queryEmbedding)
            retrieveAsPassages(bookId, scored, topPassages)
        } catch (e: Exception) {
            Timber.e(e, "Error during retrieval for bookId: $bookId")
            emptyList()
        }
    }

    suspend fun retrieveWithExpansion(
        bookId: String,
        queries: List<String>,
        entities: List<String> = emptyList(),
        boostedKeywords: List<String> = emptyList(),
        topPassages: Int = DEFAULT_TOP_PASSAGES,
        spoilerLockEnabled: Boolean = true,
        candidatesPerQuery: Int = 20
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val allCandidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()

            val candidates = if (entities.isNotEmpty()) {
                val ftsPositions = resolveFtsPositions(bookId, entities, boostedKeywords)
                if (ftsPositions.isNotEmpty()) {
                    val filtered = allCandidates.filter {
                        (it.chapterIndex to it.chunkIndex) in ftsPositions
                    }
                    filtered.ifEmpty { allCandidates }
                } else {
                    allCandidates
                }
            } else {
                allCandidates
            }

            val mergedMap = mutableMapOf<Pair<Int, Int>, Float>()

            for ((_, query) in queries.withIndex()) {
                val queryEmbedding = embedQuery(query) ?: continue

                val scored = scoreVectorOnly(candidates, queryEmbedding)

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

    private suspend fun resolveFtsPositions(
        bookId: String,
        entities: List<String>,
        boostedKeywords: List<String>
    ): Set<Pair<Int, Int>> {
        return try {
            val allTerms = (entities + boostedKeywords).distinct()

            val ftsTerms = allTerms.mapNotNull { keyword ->
                val tokens = keyword.trim().lowercase()
                    .split("\\s+".toRegex())
                    .filter { it.length > 2 && it !in STOP_WORDS }
                when {
                    tokens.size > 1 -> tokens.joinToString(" NEAR/5 ") { "$it*" }
                    tokens.size == 1 -> "${tokens[0]}*"
                    else -> null
                }
            }

            if (ftsTerms.isEmpty()) return emptySet()

            val ftsQuery = ftsTerms.joinToString(" OR ")
            val results = chunkDao.searchFts(bookId, ftsQuery)
            results.map { it.chapterIndex to it.chunkIndex }.toSet()
        } catch (e: Exception) {
            Timber.e(e, "FTS position resolution failed")
            emptySet()
        }
    }

    private fun scoreVectorOnly(
        candidates: List<ChunkVectorTuple>,
        queryEmbedding: FloatArray
    ): List<Pair<Pair<Int, Int>, Float>> {
        return candidates.mapNotNull { chunk ->
            if (chunk.embedding.size != queryEmbedding.size) return@mapNotNull null
            val sim = EmbeddingEngine.cosineSimilarity(queryEmbedding, chunk.embedding)
            if (sim >= ANCHOR_MIN_SCORE) {
                (chunk.chapterIndex to chunk.chunkIndex) to sim
            } else null
        }.sortedByDescending { it.second }
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
                                chapterTitle = chunk.chapterTitle,
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
            val filtered = allChunks.filter { it.chapterIndex <= maxChapterIndex }

            filtered.ifEmpty { null }

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
        private const val ANCHOR_MIN_SCORE = 0.40f
        private const val RRF_MIN_SCORE = 0.015f

        private val STOP_WORDS = setOf(
            "the", "and", "for", "that", "this", "with", "you", "not", "are", "from",
            "your", "all", "have", "more", "was", "its", "out", "who", "what", "where",
            "when", "why", "how", "has", "but", "into", "his", "her", "she", "him",
            "they", "them", "their", "will", "would", "could", "should", "can", "did", "some"
        )
    }
}