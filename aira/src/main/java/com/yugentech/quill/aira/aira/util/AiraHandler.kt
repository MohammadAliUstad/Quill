package com.yugentech.quill.aira.aira.util

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AiraHandler(
    private val functions: FirebaseFunctions,
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

        val history = filteredHistory.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    AiraMessageRole.USER -> "user"
                    AiraMessageRole.AIRA -> "model"
                },
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }

        val userPrompt = AiraBuilder.buildUserPrompt(question, contextBlock)
        val systemPrompt = AiraBuilder.buildSystemPrompt(book.title, book.author)

        try {
            airaMessageDao.insertMessage(
                AiraMessageEntity(
                    bookId = bookId,
                    role = AiraMessageRole.USER,
                    content = question.trim()
                )
            )

            val payload = hashMapOf(
                "prompt" to userPrompt,
                "systemPrompt" to systemPrompt,
                "history" to history
            )

            val result = functions
                .getHttpsCallable("airaChat")
                .call(payload)
                .await()

            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from function")

            emit(AiraResponse.Success(response))

            airaMessageDao.insertMessage(
                AiraMessageEntity(
                    bookId = bookId,
                    role = AiraMessageRole.AIRA,
                    content = response.trim()
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "airaChat function call failed for bookId: $bookId")
            val errorMsg = when {
                e.message?.contains("MAX_TOKENS") == true ->
                    "The answer was too long. Please try asking for a summary."

                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."

                else -> "Error: ${e.message}"
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }

    private suspend fun expandQuery(question: String): List<String> {
        return try {
            val expansionPrompt = AiraBuilder.buildExpansionPrompt(question)
            val payload = hashMapOf("prompt" to expansionPrompt)

            val result = functions
                .getHttpsCallable("airaExpand")
                .call(payload)
                .await()

            @Suppress("UNCHECKED_CAST")
            val variants = (result.getData() as? Map<*, *>)?.get("variants") as? List<String>
                ?: return listOf(question)

            if (variants.isEmpty()) listOf(question) else listOf(question) + variants
        } catch (e: Exception) {
            Timber.w(e, "Query expansion failed, falling back to original question.")
            listOf(question)
        }
    }
}