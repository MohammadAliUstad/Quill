package com.yugentech.quill.aira.aira.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.yugentech.quill.aira.aira.AiraMessage
import com.yugentech.quill.aira.aira.AiraResponse
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AiraRepositoryImpl(
    private val ragRetriever: RagRetriever,
    private val chunkDao: BookChunkDao,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao,
    private val geminiApiKey: String
) : AiraRepository {

    companion object {
        private const val TAG = "QuillAira"

        private const val HISTORY_LIMIT = 6

        private val DEAD_END_PREFIXES = listOf(
            "I haven't read that part yet",
            "I don't have enough information",
            "The passages don't",
            "The passages do not",
            "There is no information",
            "The provided passages"
        )

        // 🚨 CONDITIONAL RETRIEVAL THRESHOLD
        // With k=60, Rank #1 in Vector search = 0.01639.
        // Rank #1 in FTS = 0.0245.
        // Anything >= 0.016f means we found a highly confident match on the first try.
        private const val CONFIDENCE_THRESHOLD = 0.02f
    }

    private val model by lazy {
        Log.d(TAG, "Initializing GenerativeModel (gemini-2.5-flash)")
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 4096
            }
        )
    }

    private val expansionModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 0.0f
                maxOutputTokens = 2024
            }
        )
    }

    override suspend fun isReady(bookId: String): Boolean {
        val ready = chunkDao.isBookIndexed(bookId)
        val allChunks = chunkDao.getChunksUpToChapter(bookId, Int.MAX_VALUE)
        val indexed = allChunks.map { it.chapterIndex to it.chapterTitle }
            .distinct()
            .sortedBy { it.first }
        Log.d("QuillIndex", "Indexed chapters (${indexed.size}):")
        indexed.forEach { (idx, title) ->
            Log.d("QuillIndex", "  ch=$idx title=\"$title\"")
        }
        return ready
    }

    override suspend fun isSpoilerLockEnabled(bookId: String): Boolean {
        return bookDao.getBookEntity(bookId)?.spoilerLockEnabled ?: true
    }

    override suspend fun setSpoilerLock(bookId: String, enabled: Boolean) {
        bookDao.updateSpoilerLock(bookId, enabled)
    }

    override fun getMessagesForBook(bookId: String): Flow<List<AiraMessage>> =
        airaMessageDao.getMessagesForBook(bookId).map { entities ->
            entities.map { entity ->
                AiraMessage(
                    role = when (entity.role) {
                        AiraMessageRole.USER -> AiraMessage.Role.USER
                        AiraMessageRole.AIRA -> AiraMessage.Role.AIRA
                    },
                    content = entity.content,
                    timestamp = entity.timestamp
                )
            }
        }

    override suspend fun ask(bookId: String, question: String): Flow<AiraResponse> = flow {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "▶ ASK (Stream) — bookId=$bookId")
        Log.d(TAG, "  Question: \"${question.take(120)}\"")
        val totalStart = System.currentTimeMillis()

        // ── Guards ────────────────────────────────────────────────────────
        if (!chunkDao.isBookIndexed(bookId)) {
            Log.w(TAG, "  ✗ Book not indexed — emitting IndexingNotReady")
            emit(AiraResponse.IndexingNotReady)
            return@flow
        }

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            Log.e(TAG, "  ✗ Book entity not found in DB")
            emit(AiraResponse.Error("Book not found"))
            return@flow
        }

        if (book.progressPercent == 0f && book.lastChapterIndex == 0) {
            Log.w(TAG, "  ✗ No chapters read — emitting NoChaptersRead")
            emit(AiraResponse.NoChaptersRead)
            return@flow
        }

        // ── PHASE 1: Fast Path Retrieval ──────────────────────────────────
        Log.d(TAG, "  [1/5] Executing Fast Path (Local RAG)...")
        var retrieved = ragRetriever.retrieve(
            bookId = bookId,
            query = question,
            spoilerLockEnabled = book.spoilerLockEnabled
        )

        // ── PHASE 2: Confidence Check ─────────────────────────────────────
        val topScore = retrieved.maxOfOrNull { it.score } ?: 0f
        Log.d(TAG, "  Top match RRF Score: $topScore")

        if (topScore >= CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "  [2/5] ✅ High confidence match found. Skipping query expansion.")
        } else {
            // ── PHASE 3: Fallback Path (Query Expansion) ──────────────────
            Log.d(TAG, "  [2/5] ⚠ Low confidence. Triggering Fallback Path (Query Expansion)...")
            val queries = expandQuery(question)
            retrieved = ragRetriever.retrieveWithExpansion(
                bookId = bookId,
                queries = queries,
                spoilerLockEnabled = book.spoilerLockEnabled
            )
        }

        val contextBlock = if (retrieved.isNotEmpty()) {
            retrieved.joinToString(separator = "\n\n---\n\n") { it.text }
        } else {
            "(No relevant passages found.)"
        }

        // ── History ───────────────────────────────────────────────────────
        Log.d(TAG, "  [3/5] Fetching recent message history...")
        val recentHistory = airaMessageDao.getRecentMessagesForBook(bookId)

        val filteredHistory = recentHistory
            .windowed(size = 2, step = 2, partialWindows = true)
            .filter { pair ->
                if (pair.size < 2) return@filter true
                val airaResponse = pair[1]
                if (airaResponse.role != AiraMessageRole.AIRA) return@filter true
                !isDeadEnd(airaResponse.content)
            }
            .flatten()

        val geminiHistory = filteredHistory.map { msg ->
            when (msg.role) {
                AiraMessageRole.USER -> content(role = "user") { text(msg.content) }
                AiraMessageRole.AIRA -> content(role = "model") { text(msg.content) }
            }
        }

        // ── Prompt building ───────────────────────────────────────────────
        val systemPrompt = buildSystemPrompt(book.title, book.author)
        val userPrompt = buildUserPrompt(question, contextBlock)

        // ── Gemini call (STREAMING) ───────────────────────────────────────
        Log.d(TAG, "  [5/5] Calling Gemini Stream...")
        val geminiStart = System.currentTimeMillis()

        try {
            val chat = model.startChat(
                history = listOf(content(role = "user") { text(systemPrompt) }) + geminiHistory
            )

            // Save the user's question to the database immediately
            airaMessageDao.insertMessage(
                AiraMessageEntity(
                    bookId = bookId,
                    role = AiraMessageRole.USER,
                    content = question.trim()
                )
            )

            // This variable will accumulate the chunks as they arrive
            val fullResponseBuilder = StringBuilder()

            // 🚨 Use generateContentStream instead of sendMessage
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)

                val currentText = fullResponseBuilder.toString().replace("**", "").replace("*", "")

                // Emit the accumulated text to the ViewModel as it grows
                emit(AiraResponse.Success(currentText))
            }

            // Once the stream completes, we have the full answer.
            val finalAnswer = fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()

            if (finalAnswer.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                // Persist the final, complete answer to the database
                airaMessageDao.insertMessage(
                    AiraMessageEntity(
                        bookId = bookId,
                        role = AiraMessageRole.AIRA,
                        content = finalAnswer
                    )
                )
                Log.d(TAG, "  Stream complete. Persisted answer to DB.")
            }

            val totalMs = System.currentTimeMillis() - totalStart
            Log.d(TAG, "  ✓ ASK Stream complete in ${totalMs}ms total")

        } catch (e: Exception) {
            Log.e(TAG, "  ✗ Gemini call failed: ${e.message}", e)
            if (e.message?.contains("MAX_TOKENS") == true) {
                emit(AiraResponse.Error("The answer was too long. Please try asking for a summary."))
            } else {
                emit(AiraResponse.Error("Error: ${e.message}"))
            }
        }
    }

    private fun isDeadEnd(response: String): Boolean {
        val trimmed = response.trim()
        return DEAD_END_PREFIXES.any { prefix ->
            trimmed.startsWith(prefix, ignoreCase = true)
        }
    }

    private suspend fun expandQuery(question: String): List<String> {
        return try {
            val prompt = """
                Rewrite the following question into exactly 3 search queries for retrieving passages from a book.
                Rules:
                - Use declarative phrases, not questions
                - Each variant must preserve the original meaning exactly — do not add assumptions or new information
                - Each variant should approach the concept from a genuinely different angle, not just synonym substitution
                - Output exactly 3 lines with no preamble, no numbering, no extra text whatsoever
                
                Question: $question
            """.trimIndent()

            val response = expansionModel.generateContent(prompt)
            val text = response.text?.trim() ?: return listOf(question)

            val variants = text.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(3)

            if (variants.isEmpty()) listOf(question)
            else listOf(question) + variants
        } catch (e: Exception) {
            Log.w(TAG, "  ⚠ Query expansion failed, falling back to original: ${e.message}")
            listOf(question)
        }
    }

    private fun buildSystemPrompt(title: String, author: String): String = """
        You are Aira, a reading companion for "$title" by $author.
        Answer using ONLY the passages provided below the question.
        Use plain text only. No markdown, no bold, no headers.
        Keep answers to 2-4 sentences. Be concise.
        IMPORTANT: Previous conversation history is for context only.
        Each question must be answered solely from the provided passages,
        regardless of what was said before. If the passages contain the answer, use it.
        Only say "I haven't read that part yet." if the passages genuinely do not contain
        the answer — not because previous answers said so.
    """.trimIndent()

    private fun buildUserPrompt(question: String, contextBlock: String): String = """
        PASSAGES:
        $contextBlock
        
        QUESTION:
        $question
    """.trimIndent()
}