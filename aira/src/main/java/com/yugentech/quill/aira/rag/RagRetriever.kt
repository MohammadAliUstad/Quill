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
        entities: List<String> = emptyList(),
        boostedKeywords: List<String> = emptyList(),
        topPassages: Int = DEFAULT_TOP_PASSAGES,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val allCandidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()
            val queryEmbedding = embedQuery(query) ?: return emptyList()

            // If the entity name is confirmed via FTS, narrow to only chunks that
            // literally mention it, then rank that pool by meaning with no absolute
            // score floor (every candidate is already known to be about the right
            // person). For a common character this barely narrows anything — their
            // name is everywhere; for a minor one it's the difference between
            // finding their one mention and never seeing it, since that lone chunk
            // can undershoot ANCHOR_MIN_SCORE against a generic "who is X" query.
            val ftsPositions = if (entities.isNotEmpty()) {
                resolveFtsPositions(bookId, entities, boostedKeywords)
            } else {
                emptySet()
            }
            val ftsMatched = allCandidates.filter { (it.chapterIndex to it.chunkIndex) in ftsPositions }

            val scored = if (ftsMatched.isNotEmpty()) {
                scoreCandidates(ftsMatched, queryEmbedding, minScore = null)
            } else {
                scoreCandidates(allCandidates, queryEmbedding, minScore = ANCHOR_MIN_SCORE)
            }

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

            // Same reasoning as retrieve(): narrow to the FTS-confirmed pool when one
            // exists and skip the score floor inside it, otherwise fall back to the
            // full pool with the normal floor.
            val ftsPositions = if (entities.isNotEmpty()) {
                resolveFtsPositions(bookId, entities, boostedKeywords)
            } else {
                emptySet()
            }
            val ftsMatched = allCandidates.filter { (it.chapterIndex to it.chunkIndex) in ftsPositions }
            val candidates = ftsMatched.ifEmpty { allCandidates }
            val minScore = if (ftsMatched.isEmpty()) ANCHOR_MIN_SCORE else null

            val mergedMap = mutableMapOf<Pair<Int, Int>, Float>()

            for ((_, query) in queries.withIndex()) {
                val queryEmbedding = embedQuery(query) ?: continue

                val scored = scoreCandidates(candidates, queryEmbedding, minScore)

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

    // Dedicated retrieval for the "Who is this?" quick action. Unlike retrieve()/
    // retrieveWithExpansion(), this counts FTS occurrences of the name across the WHOLE
    // book (ignoring spoiler lock) purely to judge how prominent the character is overall
    // -- counting only within the locked pool would make every character look sparse
    // early in the book, even one who turns out to be central. The chunks actually sent
    // to the LLM are still always drawn from the spoiler-locked pool; only the regime
    // decision looks past it.
    suspend fun retrieveForCharacter(
        bookId: String,
        characterName: String,
        currentChapterIndex: Int,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val lockedCandidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()

            val wholeBookPositions = resolveFtsPositions(bookId, listOf(characterName), emptyList())
            if (wholeBookPositions.isEmpty()) {
                // No literal mention anywhere in the book (name typo/OCR quirk) -- fall
                // back to the same full-pool semantic safety net retrieve() uses.
                val queryEmbedding = embedQuery("$characterName character person description role")
                    ?: return emptyList()
                val scored = scoreCandidates(lockedCandidates, queryEmbedding, minScore = ANCHOR_MIN_SCORE)
                return retrieveAsPassages(bookId, scored, DEFAULT_TOP_PASSAGES)
            }

            val lockedPositionSet = lockedCandidates.map { it.chapterIndex to it.chunkIndex }.toSet()
            val lockedMatched = wholeBookPositions.filter { it in lockedPositionSet }
                .sortedWith(compareBy({ it.first }, { it.second }))

            if (wholeBookPositions.size <= SPARSE_MATCH_THRESHOLD) {
                return fetchChunksAt(bookId, lockedMatched.map { it to CHARACTER_UNRANKED_SCORE })
            }

            // HIGH-COUNT regime: a "recent" zone anchored on the literal tapped chunk,
            // plus ranked "early"/"middle" zones from the remaining FTS-matched chunks.
            val anchor = findAnchorPosition(bookId, currentChapterIndex, characterName)
            val recentChunks = anchor?.let { fetchRecentZone(bookId, it) } ?: emptyList()
            val recentPositions = recentChunks.map { it.chapterIndex to it.chunkIndex }.toSet()

            val remaining = lockedMatched.filter { it !in recentPositions }
            val midpoint = remaining.size / 2
            val earlyPositions = remaining.subList(0, midpoint)
            val middlePositions = remaining.subList(midpoint, remaining.size)

            val queryEmbedding = embedQuery("$characterName character person description role")
            val earlyChunks = queryEmbedding?.let {
                rankAndFetch(bookId, earlyPositions, lockedCandidates, it, CHARACTER_EARLY_BUDGET)
            } ?: emptyList()
            val middleChunks = queryEmbedding?.let {
                rankAndFetch(bookId, middlePositions, lockedCandidates, it, CHARACTER_MIDDLE_BUDGET)
            } ?: emptyList()

            val seen = mutableSetOf<Pair<Int, Int>>()
            (recentChunks + earlyChunks + middleChunks)
                .filter { seen.add(it.chapterIndex to it.chunkIndex) }
                .sortedWith(compareBy({ it.chapterIndex }, { it.chunkIndex }))
        } catch (e: Exception) {
            Timber.e(e, "Error during character retrieval for bookId: $bookId, character: $characterName")
            emptyList()
        }
    }

    private suspend fun findAnchorPosition(
        bookId: String,
        currentChapterIndex: Int,
        characterName: String
    ): Pair<Int, Int>? {
        return try {
            chunkDao.getChunksForChapter(bookId, currentChapterIndex)
                .firstOrNull { it.text.contains(characterName, ignoreCase = true) }
                ?.let { it.chapterIndex to it.chunkIndex }
        } catch (e: Exception) {
            Timber.e(e, "Failed to find anchor chunk for character: $characterName")
            null
        }
    }

    private suspend fun fetchRecentZone(
        bookId: String,
        anchor: Pair<Int, Int>
    ): List<RetrievedChunk> {
        return try {
            val (chapterIndex, chunkIndex) = anchor
            val fromChunkIndex = (chunkIndex - CHARACTER_RECENT_LOOKBACK).coerceAtLeast(0)
            chunkDao.getNeighborChunks(bookId, chapterIndex, fromChunkIndex, chunkIndex).map { chunk ->
                RetrievedChunk(
                    text = chunk.text,
                    chapterIndex = chunk.chapterIndex,
                    chapterTitle = chunk.chapterTitle,
                    chunkIndex = chunk.chunkIndex,
                    score = CHARACTER_UNRANKED_SCORE
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch recent zone at $anchor")
            emptyList()
        }
    }

    private suspend fun rankAndFetch(
        bookId: String,
        positions: List<Pair<Int, Int>>,
        candidates: List<ChunkVectorTuple>,
        queryEmbedding: FloatArray,
        budget: Int
    ): List<RetrievedChunk> {
        if (positions.isEmpty()) return emptyList()
        val positionSet = positions.toSet()
        val pool = candidates.filter { (it.chapterIndex to it.chunkIndex) in positionSet }
        val scored = scoreCandidates(pool, queryEmbedding, minScore = null).take(budget)
        return fetchChunksAt(bookId, scored)
    }

    private suspend fun fetchChunksAt(
        bookId: String,
        scoredPositions: List<Pair<Pair<Int, Int>, Float>>
    ): List<RetrievedChunk> {
        val result = mutableListOf<RetrievedChunk>()
        for ((pos, score) in scoredPositions) {
            try {
                val chunk = chunkDao.getNeighborChunks(bookId, pos.first, pos.second, pos.second).firstOrNull()
                if (chunk != null) {
                    result.add(
                        RetrievedChunk(
                            text = chunk.text,
                            chapterIndex = chunk.chapterIndex,
                            chapterTitle = chunk.chapterTitle,
                            chunkIndex = chunk.chunkIndex,
                            score = score
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch chunk at $pos")
            }
        }
        return result
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

    // Ranks candidates by semantic similarity. minScore == null means every candidate
    // is kept (used for an FTS-confirmed pool, where lexical match already proved
    // relevance); otherwise only candidates clearing minScore survive.
    private fun scoreCandidates(
        candidates: List<ChunkVectorTuple>,
        queryEmbedding: FloatArray,
        minScore: Float?
    ): List<Pair<Pair<Int, Int>, Float>> {
        return candidates.mapNotNull { chunk ->
            if (chunk.embedding.size != queryEmbedding.size) return@mapNotNull null
            val sim = EmbeddingEngine.cosineSimilarity(queryEmbedding, chunk.embedding)
            if (minScore == null || sim >= minScore) {
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

        private const val SPARSE_MATCH_THRESHOLD = 6
        private const val CHARACTER_RECENT_LOOKBACK = 5
        private const val CHARACTER_EARLY_BUDGET = 8
        private const val CHARACTER_MIDDLE_BUDGET = 6
        private const val CHARACTER_UNRANKED_SCORE = 1.0f

        private val STOP_WORDS = setOf(
            "the", "and", "for", "that", "this", "with", "you", "not", "are", "from",
            "your", "all", "have", "more", "was", "its", "out", "who", "what", "where",
            "when", "why", "how", "has", "but", "into", "his", "her", "she", "him",
            "they", "them", "their", "will", "would", "could", "should", "can", "did", "some"
        )
    }
}