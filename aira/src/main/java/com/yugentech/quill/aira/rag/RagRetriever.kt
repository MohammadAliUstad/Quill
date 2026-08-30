package com.yugentech.quill.aira.rag

import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.ChunkVectorTuple
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.RetrievedChunk
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
            emptyList()
        }
    }

    // "Recall Character" retrieval: literal FTS matches for the tapped name, from the
    // book start through the reader's locked chapters. Corrections applied on top of the
    // raw match list before it's capped:
    // 1. Adjacent-match dedupe: chunks overlap by 250 characters (see ChunkingStrategy),
    //    so a mention sitting in that overlap can get FTS-matched in two consecutive
    //    chunks for what is really one mention in the narrative. Collapsing adjacent
    //    chunkIndex matches within the same chapter keeps the budget from being spent on
    //    echoes of the same passage.
    // 2. Progress-proportional zoning: a plain chronological walk, even with a per-chapter
    //    cap, still exhausts the whole budget inside however many chapters happen to sit
    //    earliest -- a busy character can supply capped-out matches for chapter after
    //    chapter starting right from their introduction. That starves every chapter from
    //    partway through the reader's progress onward, so a reader near the end of a long
    //    book would get an answer frozen at the character's earliest pages. Splitting the
    //    intro-to-current-progress span into equal chapter-width zones and giving each an
    //    even slice of the budget forces the selection to track how far the reader has
    //    actually read, not just where the character happens to be introduced.
    // 3. Semantic re-ranking within each zone: FTS only proves the name is *mentioned* in
    //    a chunk, not that the chunk is actually about them -- a character can spend whole
    //    chapters as a passive listener to someone else's story (a monologue, a subplot
    //    they're merely present for), and those chunks are just as likely to win a
    //    chronological pick as one that actually describes them. Scoring each zone's
    //    candidates against a handful of fixed identity-facet queries and keeping the
    //    top-ranked ones (still capped per chapter) favors chunks that substantively
    //    describe the character over ones where they're just nearby.
    suspend fun retrieveForRecallCharacter(
        bookId: String,
        characterName: String,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val maxChapterIndex = if (spoilerLockEnabled) {
                (book?.lastChapterIndex ?: 0) + 1
            } else {
                Int.MAX_VALUE
            }

            val matched = resolveFtsPositionsUpToChapter(
                bookId, listOf(characterName), emptyList(), maxChapterIndex
            )
            if (matched.isEmpty()) return emptyList()

            val deduped = dedupeAdjacentOverlap(matched)

            val selected = if (deduped.size <= RECALL_CHARACTER_BUDGET) {
                deduped
            } else {
                selectAcrossProgressZones(bookId, characterName, deduped, maxChapterIndex)
            }

            fetchChunksAt(bookId, selected.map { it to CHARACTER_UNRANKED_SCORE })
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun selectAcrossProgressZones(
        bookId: String,
        characterName: String,
        deduped: List<Pair<Int, Int>>,
        maxChapterIndex: Int
    ): List<Pair<Int, Int>> {
        val firstChapter = deduped.first().first
        val span = (maxChapterIndex - firstChapter + 1).coerceAtLeast(1)
        val zoneWidth = (span + RECALL_CHARACTER_ZONE_COUNT - 1) / RECALL_CHARACTER_ZONE_COUNT
        val zoneBudget = RECALL_CHARACTER_BUDGET / RECALL_CHARACTER_ZONE_COUNT

        // The very first exact mention(s) are usually the character's actual introduction --
        // the single most information-dense passage about them -- so they're taken directly
        // rather than left to compete purely on semantic score, which could in principle rank
        // them below some other chunk in the same zone.
        val guaranteed = deduped.take(RECALL_CHARACTER_GUARANTEED_FIRST_MENTIONS)
        val guaranteedPositions = guaranteed.toSet()
        val remaining = deduped.filter { it !in guaranteedPositions }

        fun zoneIndexOf(pos: Pair<Int, Int>) =
            ((pos.first - firstChapter) / zoneWidth).coerceIn(0, RECALL_CHARACTER_ZONE_COUNT - 1)

        val guaranteedByZone = guaranteed.groupBy(::zoneIndexOf)

        val facetEmbeddings = RECALL_CHARACTER_FACET_QUERIES.mapNotNull { facet ->
            embedQuery(facet(characterName))
        }
        val vectorsByPosition = chunkDao.getCandidateVectorsInRange(bookId, firstChapter, maxChapterIndex)
            .associateBy { it.chapterIndex to it.chunkIndex }

        val zones = List(RECALL_CHARACTER_ZONE_COUNT) { mutableListOf<Pair<Int, Int>>() }
        for (pos in remaining) {
            zones[zoneIndexOf(pos)].add(pos)
        }

        val selected = mutableListOf<Pair<Int, Int>>()
        selected.addAll(guaranteed)

        zones.forEachIndexed { zoneIndex, zone ->
            val ranked = if (facetEmbeddings.isEmpty()) {
                zone
            } else {
                zone.mapNotNull { pos ->
                    val vector = vectorsByPosition[pos] ?: return@mapNotNull null
                    val bestFacetScore = facetEmbeddings.maxOf { facetEmbedding ->
                        EmbeddingEngine.cosineSimilarity(facetEmbedding, vector.embedding)
                    }
                    pos to bestFacetScore
                }.sortedByDescending { it.second }.map { it.first }.ifEmpty { zone }
            }

            val guaranteedInZone = guaranteedByZone[zoneIndex].orEmpty()
            val perChapterCount = guaranteedInZone.groupingBy { it.first }.eachCount().toMutableMap()
            val budgetForZone = (zoneBudget - guaranteedInZone.size).coerceAtLeast(0)

            var zoneSelected = 0
            for (pos in ranked) {
                if (zoneSelected >= budgetForZone) break
                val count = perChapterCount.getOrDefault(pos.first, 0)
                if (count >= RECALL_CHARACTER_PER_CHAPTER_CAP) continue
                selected.add(pos)
                perChapterCount[pos.first] = count + 1
                zoneSelected++
            }
        }

        return selected.sortedWith(compareBy({ it.first }, { it.second }))
    }

    // "Recent Role" retrieval: pure recency, no semantic ranking, no whole-book scan --
    // FTS is scoped directly to the reader's unlocked chapters at the query level (not
    // filtered afterward), since there's nothing to gain from searching chapters that
    // will just be thrown away. A character's current situation lives in whatever they
    // were most recently doing, so we simply take the latest matches within that pool.
    suspend fun retrieveForRecentRole(
        bookId: String,
        characterName: String,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val maxChapterIndex = if (spoilerLockEnabled) {
                (book?.lastChapterIndex ?: 0) + 1
            } else {
                Int.MAX_VALUE
            }

            val lockedMatched = resolveFtsPositionsUpToChapter(
                bookId, listOf(characterName), emptyList(), maxChapterIndex
            )
            if (lockedMatched.isEmpty()) return emptyList()

            val recent = lockedMatched.takeLast(RECENT_ROLE_BUDGET)
            fetchChunksAt(bookId, recent.map { it to CHARACTER_UNRANKED_SCORE })
        } catch (e: Exception) {
            emptyList()
        }
    }

    // "Journey So Far" retrieval: a deliberately time-spanning trace of the character from
    // their first mention up to the reader's progress. The span (measured in chunks, not
    // chapters, so short and long chapters weigh fairly) is split 20/60/20:
    // - Early 20% (JOURNEY_EDGE_BUDGET): the first few mentions verbatim -- the character's
    //   introduction -- then the best-ranked rest of the zone.
    // - Recent 20% (JOURNEY_EDGE_BUDGET): the latest few mentions verbatim -- where they are
    //   right now -- then the best-ranked rest of the zone.
    // - Middle 60% (everything left of JOURNEY_BUDGET): split into sub-zones and picked
    //   round-robin, best-ranked first within each, with a per-chapter cap, so the middle of
    //   the arc is scattered across the whole stretch instead of clustering in one chapter.
    // Disparateness across zones is the intended format here, not something to avoid.
    suspend fun retrieveForJourney(
        bookId: String,
        characterName: String,
        spoilerLockEnabled: Boolean = true
    ): List<RetrievedChunk> {
        return try {
            val book = bookDao.getBookEntity(bookId)
            val lockedCandidates = getCandidates(bookId, book, spoilerLockEnabled) ?: return emptyList()

            val maxChapterIndex = if (spoilerLockEnabled) {
                (book?.lastChapterIndex ?: 0) + 1
            } else {
                Int.MAX_VALUE
            }
            val matched = resolveFtsPositionsUpToChapter(
                bookId, listOf(characterName), emptyList(), maxChapterIndex
            )
            if (matched.isEmpty()) return emptyList()

            val deduped = dedupeAdjacentOverlap(matched)
            val selected = if (deduped.size <= JOURNEY_BUDGET) {
                deduped
            } else {
                selectJourneyZones(characterName, deduped, lockedCandidates)
            }

            fetchChunksAt(bookId, selected.map { it to CHARACTER_UNRANKED_SCORE })
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun selectJourneyZones(
        characterName: String,
        deduped: List<Pair<Int, Int>>,
        candidates: List<ChunkVectorTuple>
    ): List<Pair<Int, Int>> {
        val ordered = candidates.sortedWith(compareBy({ it.chapterIndex }, { it.chunkIndex }))
        val ordinalOf = ordered.withIndex()
            .associate { (index, chunk) -> (chunk.chapterIndex to chunk.chunkIndex) to index }
        val vectorOf = ordered.associateBy { it.chapterIndex to it.chunkIndex }

        val mentions = deduped.filter { it in ordinalOf }
        if (mentions.size <= JOURNEY_BUDGET) return mentions

        // Span runs from the first mention to the end of the reader's unlocked text.
        val startOrdinal = ordinalOf.getValue(mentions.first())
        val endOrdinal = ordered.lastIndex
        val span = endOrdinal - startOrdinal + 1
        val edgeWidth = (span * JOURNEY_EDGE_FRACTION).toInt().coerceAtLeast(1)
        val earlyEnd = startOrdinal + edgeWidth                      // exclusive
        val lateStart = (endOrdinal - edgeWidth + 1).coerceAtLeast(earlyEnd)

        fun ordinal(pos: Pair<Int, Int>) = ordinalOf.getValue(pos)
        val early = mentions.filter { ordinal(it) < earlyEnd }
        val late = mentions.filter { ordinal(it) >= lateStart }
        val middle = mentions.filter { ordinal(it) in earlyEnd until lateStart }

        val facetEmbeddings = JOURNEY_FACET_QUERIES.mapNotNull { facet -> embedQuery(facet(characterName)) }
        fun score(pos: Pair<Int, Int>): Float {
            val vector = vectorOf[pos] ?: return 0f
            if (facetEmbeddings.isEmpty()) return 0f
            return facetEmbeddings.maxOf { EmbeddingEngine.cosineSimilarity(it, vector.embedding) }
        }
        fun ranked(positions: List<Pair<Int, Int>>) = positions.sortedByDescending(::score)

        val selected = linkedSetOf<Pair<Int, Int>>()

        fun fillEdge(zone: List<Pair<Int, Int>>, verbatim: List<Pair<Int, Int>>) {
            selected.addAll(verbatim)
            val rest = zone.filter { it !in selected }
            selected.addAll(ranked(rest).take(JOURNEY_EDGE_BUDGET - verbatim.size))
        }
        fillEdge(early, early.take(JOURNEY_EDGE_VERBATIM))
        fillEdge(late, late.takeLast(JOURNEY_EDGE_VERBATIM))

        // Middle gets whatever budget the edges didn't use.
        var middleBudget = JOURNEY_BUDGET - selected.size
        val middleWidth = ((lateStart - earlyEnd + JOURNEY_MIDDLE_SUBZONES - 1) / JOURNEY_MIDDLE_SUBZONES)
            .coerceAtLeast(1)
        val subZones = List(JOURNEY_MIDDLE_SUBZONES) { mutableListOf<Pair<Int, Int>>() }
        for (pos in middle) {
            val zoneIndex = ((ordinal(pos) - earlyEnd) / middleWidth).coerceIn(0, JOURNEY_MIDDLE_SUBZONES - 1)
            subZones[zoneIndex].add(pos)
        }
        val queues = subZones.map { ArrayDeque(ranked(it)) }
        val perChapterCount = mutableMapOf<Int, Int>()
        while (middleBudget > 0 && queues.any { it.isNotEmpty() }) {
            for (queue in queues) {
                if (middleBudget == 0) break
                while (queue.isNotEmpty()) {
                    val pos = queue.removeFirst()
                    val count = perChapterCount.getOrDefault(pos.first, 0)
                    if (count >= JOURNEY_MIDDLE_PER_CHAPTER_CAP) continue
                    selected.add(pos)
                    perChapterCount[pos.first] = count + 1
                    middleBudget--
                    break
                }
            }
        }

        // If a zone was too thin to use its share (e.g. a sparse middle), top up from the
        // best-ranked leftovers anywhere in the span so the full budget still goes out.
        if (selected.size < JOURNEY_BUDGET) {
            val leftovers = mentions.filter { it !in selected }
            selected.addAll(ranked(leftovers).take(JOURNEY_BUDGET - selected.size))
        }

        return selected.sortedWith(compareBy({ it.first }, { it.second }))
    }

    // Chunks overlap by 250 characters (see ChunkingStrategy), so a mention sitting in that
    // overlap can get FTS-matched in two consecutive chunks for what is really one mention.
    // Collapsing adjacent chunkIndex matches within the same chapter keeps the budget from
    // being spent on echoes of the same passage.
    private fun dedupeAdjacentOverlap(matched: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val deduped = mutableListOf<Pair<Int, Int>>()
        for (pos in matched) {
            val last = deduped.lastOrNull()
            val isAdjacentOverlap = last != null && last.first == pos.first && pos.second - last.second == 1
            if (!isAdjacentOverlap) deduped.add(pos)
        }
        return deduped
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
            val ftsQuery = buildFtsQuery(entities, boostedKeywords) ?: return emptySet()
            val results = chunkDao.searchFts(bookId, ftsQuery)
            results.map { it.chapterIndex to it.chunkIndex }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // Same as resolveFtsPositions, but scopes the FTS query itself to chapters at or
    // before maxChapterIndex -- for callers that only ever need the locked pool, this
    // avoids searching (and immediately discarding) chapters the reader hasn't reached.
    // Returns a list, already sorted chronologically, since every caller of this variant
    // cares about chunk order.
    private suspend fun resolveFtsPositionsUpToChapter(
        bookId: String,
        entities: List<String>,
        boostedKeywords: List<String>,
        maxChapterIndex: Int
    ): List<Pair<Int, Int>> {
        return try {
            val ftsQuery = buildFtsQuery(entities, boostedKeywords) ?: return emptyList()
            val results = chunkDao.searchFtsUpToChapter(bookId, ftsQuery, maxChapterIndex)
            results.map { it.chapterIndex to it.chunkIndex }
                .sortedWith(compareBy({ it.first }, { it.second }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildFtsQuery(entities: List<String>, boostedKeywords: List<String>): String? {
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

        if (ftsTerms.isEmpty()) return null
        return ftsTerms.joinToString(" OR ")
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
            null
        }
    }

    companion object {
        const val DEFAULT_TOP_PASSAGES = 3
        private const val PASSAGE_WINDOW_BEFORE = 1
        private const val PASSAGE_WINDOW_AFTER = 1
        private const val ANCHOR_MIN_SCORE = 0.40f
        private const val RRF_MIN_SCORE = 0.015f

        private const val CHARACTER_UNRANKED_SCORE = 1.0f

        // retrieveForRecallCharacter
        private const val RECALL_CHARACTER_BUDGET = 50
        private const val RECALL_CHARACTER_PER_CHAPTER_CAP = 4
        private const val RECALL_CHARACTER_ZONE_COUNT = 5
        private const val RECALL_CHARACTER_GUARANTEED_FIRST_MENTIONS = 2
        private val RECALL_CHARACTER_FACET_QUERIES: List<(String) -> String> = listOf(
            { name -> "$name personality character traits temperament" },
            { name -> "$name role occupation position in the story background" },
            { name -> "$name relationships family friends other characters" },
            { name -> "$name physical appearance introduction description" }
        )

        // retrieveForRecentRole
        private const val RECENT_ROLE_BUDGET = 50

        // retrieveForJourney
        private const val JOURNEY_BUDGET = 50
        private const val JOURNEY_EDGE_FRACTION = 0.20f
        private const val JOURNEY_EDGE_BUDGET = 10
        private const val JOURNEY_EDGE_VERBATIM = 4
        private const val JOURNEY_MIDDLE_SUBZONES = 6
        private const val JOURNEY_MIDDLE_PER_CHAPTER_CAP = 4
        private val JOURNEY_FACET_QUERIES: List<(String) -> String> = listOf(
            { name -> "$name decision turning point change of heart" },
            { name -> "$name conflict struggle confrontation crisis" },
            { name -> "$name relationship with others grows changes breaks" },
            { name -> "$name arrives leaves goes events happen to them" }
        )

        private val STOP_WORDS = setOf(
            "the", "and", "for", "that", "this", "with", "you", "not", "are", "from",
            "your", "all", "have", "more", "was", "its", "out", "who", "what", "where",
            "when", "why", "how", "has", "but", "into", "his", "her", "she", "him",
            "they", "them", "their", "will", "would", "could", "should", "can", "did",
            "some", "he", "it"
        )
    }
}