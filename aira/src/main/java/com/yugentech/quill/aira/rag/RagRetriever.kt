package com.yugentech.quill.aira.rag

import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.ChunkVectorTuple
import com.yugentech.quill.database.entity.BookEntity
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
            Timber.d("[RagRetriever] Starting retrieval | queries: ${queries.size} | topPassages: $topPassages | candidatesPerQuery: $candidatesPerQuery | boostedKeywords: $boostedKeywords")

            val book = bookDao.getBookEntity(bookId)
            val allCandidates = getCandidates(bookId, book, spoilerLockEnabled) ?: run {
                Timber.w("[RagRetriever] No candidates returned — spoiler lock may have filtered everything")
                return emptyList()
            }

            Timber.d("[RagRetriever] Candidate pool size: ${allCandidates.size} chunks")

            val candidates = if (entities.isNotEmpty()) {
                val ftsPositions = resolveFtsPositions(bookId, entities, boostedKeywords)
                if (ftsPositions.isNotEmpty()) {
                    val filtered = allCandidates.filter {
                        (it.chapterIndex to it.chunkIndex) in ftsPositions
                    }
                    Timber.d("[RagRetriever] Path A (entity): FTS filtered ${allCandidates.size} → ${filtered.size} candidates")
                    filtered.ifEmpty {
                        Timber.w("[RagRetriever] FTS filter returned empty subset, falling back to full corpus")
                        allCandidates
                    }
                } else {
                    Timber.w("[RagRetriever] FTS returned no positions, falling back to full corpus")
                    allCandidates
                }
            } else {
                Timber.d("[RagRetriever] Path B (thematic): no entities, using full corpus of ${allCandidates.size} chunks")
                allCandidates
            }

            val mergedMap = mutableMapOf<Pair<Int, Int>, Float>()

            for ((index, query) in queries.withIndex()) {
                Timber.d(
                    "[RagRetriever] Processing query ${index + 1}/${queries.size}: \"${
                        query.take(
                            80
                        )
                    }\""
                )

                val queryEmbedding = embedQuery(query) ?: run {
                    Timber.w("[RagRetriever] Embedding failed for query ${index + 1}, skipping")
                    continue
                }

                val scored = scoreVectorOnly(candidates, queryEmbedding)

                Timber.d(
                    "[RagRetriever] Query ${index + 1} scored ${scored.size} chunks | top 5: ${
                        scored.take(
                            5
                        ).map { "ch${it.first.first}(%.4f)".format(it.second) }
                    }"
                )

                scored.take(candidatesPerQuery).forEach { (pos, score) ->
                    val existing = mergedMap[pos]
                    if (existing == null || score > existing) {
                        mergedMap[pos] = score
                    }
                }
            }

            Timber.d(
                "[RagRetriever] Merged map size: ${mergedMap.size} | top 5: ${
                    mergedMap.entries.sortedByDescending { it.value }.take(5)
                        .map { "ch${it.key.first}(%.4f)".format(it.value) }
                }"
            )

            if (mergedMap.isEmpty()) {
                Timber.w("[RagRetriever] Merged map is empty — no chunks survived scoring")
                return emptyList()
            }

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
                    .filter { it.length > 2 }
                when {
                    tokens.size > 1 -> tokens.joinToString(" NEAR/5 ") { "$it*" }
                    tokens.size == 1 -> "${tokens[0]}*"
                    else -> null
                }
            }

            if (ftsTerms.isEmpty()) return emptySet()

            val ftsQuery = ftsTerms.joinToString(" OR ")
            Timber.d("[RagRetriever] FTS boolean filter query: \"$ftsQuery\"")

            val results = chunkDao.searchFts(bookId, ftsQuery)
            Timber.d(
                "[RagRetriever] FTS positions: ${results.size} chunks | chapters: ${
                    results.map { "ch${it.chapterIndex}" }.distinct()
                }"
            )

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
        val results = candidates.mapNotNull { chunk ->
            if (chunk.embedding.size != queryEmbedding.size) return@mapNotNull null
            val sim = EmbeddingEngine.cosineSimilarity(queryEmbedding, chunk.embedding)
            if (sim >= ANCHOR_MIN_SCORE) {
                (chunk.chapterIndex to chunk.chunkIndex) to sim
            } else null
        }.sortedByDescending { it.second }

        Timber.d(
            "[RagRetriever] Vector search: ${results.size} chunks above ANCHOR_MIN_SCORE($ANCHOR_MIN_SCORE) | top 5: ${
                results.take(
                    5
                ).map { "ch${it.first.first}(%.4f)".format(it.second) }
            }"
        )
        return results
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

        Timber.d(
            "[RagRetriever] Anchors selected: ${
                anchors.map {
                    "ch${it.first.first}/chunk${it.first.second}(%.4f)".format(
                        it.second
                    )
                }
            }"
        )

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

        val result = expanded.sortedWith(compareBy({ it.chapterIndex }, { it.chunkIndex }))
        Timber.d("[RagRetriever] Final chunks returned: ${result.map { "ch${it.chapterIndex}" }}")
        return result
    }

    private suspend fun getCandidates(
        bookId: String,
        book: BookEntity?,
        spoilerLockEnabled: Boolean
    ): List<ChunkVectorTuple>? {
        return try {
            val allChunks = cacheMutex.withLock {
                if (cachedBookId == bookId && cachedVectors.isNotEmpty()) {
                    Timber.d("[RagRetriever] Using cached vectors for bookId: $bookId (${cachedVectors.size} chunks)")
                    cachedVectors
                } else {
                    Timber.d("[RagRetriever] Loading vectors from DB for bookId: $bookId")
                    val fromDb = chunkDao.getCandidateVectors(bookId, Int.MAX_VALUE)
                    cachedBookId = bookId
                    cachedVectors = fromDb
                    Timber.d("[RagRetriever] Loaded ${fromDb.size} chunks from DB")
                    fromDb
                }
            }

            if (allChunks.isEmpty()) {
                Timber.w("[RagRetriever] No chunks in DB for bookId: $bookId")
                return null
            }

            if (!spoilerLockEnabled) {
                Timber.d("[RagRetriever] Spoiler lock disabled — using all ${allChunks.size} chunks")
                return allChunks
            }

            val progressCeiling = book?.progressPercent ?: 0f
            if (progressCeiling == 0f) {
                Timber.w("[RagRetriever] Progress is 0% — spoiler lock blocking all chunks")
                return null
            }

            val maxChapterIndex = (book?.lastChapterIndex ?: 0) + 1
            val filtered = allChunks.filter { it.chapterIndex <= maxChapterIndex }
            Timber.d("[RagRetriever] Spoiler lock active | maxChapterIndex: $maxChapterIndex | filtered to ${filtered.size} chunks")

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
            val embedding = embeddingEngine.embed(queryWithPrefix)
            Timber.d("[RagRetriever] Embedded query: \"${cleanQuery.take(60)}\" → ${embedding?.size ?: 0} dims")
            embedding
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