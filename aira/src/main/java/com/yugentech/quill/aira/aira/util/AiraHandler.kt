package com.yugentech.quill.aira.aira.util

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class AiraHandler(
    private val model: GenerativeModel,
    private val expansionModel: GenerativeModel,
    private val ragRetriever: RagRetriever,
    private val bookDao: BookDao,
    private val bookChunkDao: BookChunkDao,
    private val airaMessageDao: AiraMessageDao
) {

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.02f
    }

    fun ask(bookId: String, question: String): Flow<AiraResponse> = flow {
        if (!bookChunkDao.isBookIndexed(bookId)) {
            emit(AiraResponse.Error("Book is still indexing. Please wait."))
            return@flow
        }

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            emit(AiraResponse.Error("Book not found."))
            return@flow
        }

        if (book.progressPercent == 0f && book.lastChapterIndex == 0) {
            emit(AiraResponse.Error("Start reading to unlock Aira's knowledge."))
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

        val contextBlock = AiraBuilder.buildContextBlock(retrieved.map { it.text })
        val recentHistory = airaMessageDao.getRecentMessagesForBook(bookId)

        val filteredHistory = recentHistory
            .windowed(size = 2, step = 2, partialWindows = true)
            .filter { pair ->
                if (pair.size < 2) return@filter true
                val airaResponse = pair[1]
                if (airaResponse.role != AiraMessageRole.AIRA) return@filter true
                !AiraBuilder.isDeadEnd(airaResponse.content)
            }
            .flatten()

        val geminiHistory = filteredHistory.map { msg ->
            when (msg.role) {
                AiraMessageRole.USER -> content(role = "user") { text(msg.content) }
                AiraMessageRole.AIRA -> content(role = "model") { text(msg.content) }
            }
        }

        val userPrompt = AiraBuilder.buildUserPrompt(question, contextBlock)
        val systemPrompt = AiraBuilder.buildSystemPrompt(book.title, book.author)

        try {
            val chat = model.startChat(
                history = listOf(content(role = "user") { text(systemPrompt) }) + geminiHistory
            )

            airaMessageDao.insertMessage(
                AiraMessageEntity(
                    bookId = bookId,
                    role = AiraMessageRole.USER,
                    content = question.trim()
                )
            )

            val fullResponseBuilder = StringBuilder()
            chat.sendMessageStream(userPrompt).collect { chunk ->
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)
                emit(
                    AiraResponse.Success(
                        fullResponseBuilder.toString().replace("**", "").replace("*", "")
                    )
                )
            }

            val finalAnswer =
                fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()

            if (finalAnswer.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                airaMessageDao.insertMessage(
                    AiraMessageEntity(
                        bookId = bookId,
                        role = AiraMessageRole.AIRA,
                        content = finalAnswer
                    )
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "Gemini chat stream failed for bookId: $bookId")
            val errorMsg = if (e.message?.contains("MAX_TOKENS") == true) {
                "The answer was too long. Please try asking for a summary."
            } else {
                "Error: ${e.message}"
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }

    private suspend fun expandQuery(question: String): List<String> {
        return try {
            val prompt = AiraBuilder.buildExpansionPrompt(question)
            val response = expansionModel.generateContent(prompt)
            val text = response.text?.trim() ?: return listOf(question)
            val variants = text.lines().map { it.trim() }.filter { it.isNotBlank() }.take(3)

            if (variants.isEmpty()) listOf(question) else listOf(question) + variants
        } catch (e: Exception) {
            Timber.w(e, "Query expansion failed, safely falling back to original question.")
            listOf(question)
        }
    }
}