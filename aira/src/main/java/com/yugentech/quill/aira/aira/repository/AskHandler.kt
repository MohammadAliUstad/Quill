package com.yugentech.quill.aira.aira.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.yugentech.quill.aira.aira.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import com.yugentech.quill.aira.rag.RagRetriever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AskHandler(
    private val ragRetriever: RagRetriever,
    private val chunkDao: BookChunkDao,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao,
    private val model: GenerativeModel,
    private val expansionModel: GenerativeModel
) {

    companion object {
        private const val TAG = "QuillAira"
        private const val CONFIDENCE_THRESHOLD = 0.02f

        private val DEAD_END_PREFIXES = listOf(
            "I haven't read that part yet",
            "I don't have enough information",
            "The passages don't",
            "The passages do not",
            "There is no information",
            "The provided passages"
        )
    }

    fun ask(bookId: String, question: String): Flow<AiraResponse> = flow {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "▶ ASK (Stream) — bookId=$bookId")
        Log.d(TAG, "  Question: \"${question.take(120)}\"")
        val totalStart = System.currentTimeMillis()

        if (!chunkDao.isBookIndexed(bookId)) {
            emit(AiraResponse.IndexingNotReady)
            return@flow
        }

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            emit(AiraResponse.Error("Book not found"))
            return@flow
        }

        if (book.progressPercent == 0f && book.lastChapterIndex == 0) {
            emit(AiraResponse.NoChaptersRead)
            return@flow
        }

        var retrieved = ragRetriever.retrieve(
            bookId = bookId,
            query = question,
            spoilerLockEnabled = book.spoilerLockEnabled
        )

        val topScore = retrieved.maxOfOrNull { it.score } ?: 0f
        if (topScore < CONFIDENCE_THRESHOLD) {
            val queries = expandQuery(question)
            retrieved = ragRetriever.retrieveWithExpansion(
                bookId = bookId,
                queries = queries,
                spoilerLockEnabled = book.spoilerLockEnabled
            )
        }

        val contextBlock = if (retrieved.isNotEmpty())
            retrieved.joinToString(separator = "\n\n---\n\n") { it.text }
        else
            "(No relevant passages found.)"

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

        val systemPrompt = PromptBuilder.buildSystemPrompt(book.title, book.author)
        val userPrompt = PromptBuilder.buildUserPrompt(question, contextBlock)

        try {
            val chat = model.startChat(
                history = listOf(content(role = "user") { text(systemPrompt) }) + geminiHistory
            )

            airaMessageDao.insertMessage(
                AiraMessageEntity(bookId = bookId, role = AiraMessageRole.USER, content = question.trim())
            )

            val fullResponseBuilder = StringBuilder()
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)
                emit(AiraResponse.Success(fullResponseBuilder.toString().replace("**", "").replace("*", "")))
            }

            val finalAnswer = fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()
            if (finalAnswer.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                airaMessageDao.insertMessage(
                    AiraMessageEntity(bookId = bookId, role = AiraMessageRole.AIRA, content = finalAnswer)
                )
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
            val variants = text.lines().map { it.trim() }.filter { it.isNotBlank() }.take(3)
            if (variants.isEmpty()) listOf(question) else listOf(question) + variants
        } catch (e: Exception) {
            Log.w(TAG, "  ⚠ Query expansion failed: ${e.message}")
            listOf(question)
        }
    }

    private fun isDeadEnd(response: String): Boolean {
        val trimmed = response.trim()
        return DEAD_END_PREFIXES.any { prefix -> trimmed.startsWith(prefix, ignoreCase = true) }
    }
}