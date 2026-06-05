package com.yugentech.quill.aira.aira.util

import com.yugentech.quill.aira.bookChat.handler.BookChatHandler
import com.yugentech.quill.aira.generalChat.handler.GeneralChatHandler
import com.yugentech.quill.aira.intentDetection.model.Intent
import com.yugentech.quill.aira.intentDetection.repository.IntentDetectionRepository
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiraHandler(
    private val intentDetectionRepository: IntentDetectionRepository,
    private val generalChatHandler: GeneralChatHandler,
    private val bookChatHandler: BookChatHandler,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao
) {

    fun ask(bookId: String, question: String): Flow<AiraResponse> = flow {
        val book = bookDao.getBookEntity(bookId)!!
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

        try {
            val intent = intentDetectionRepository.detectIntent(
                question = question,
                title = book.title,
                author = book.author
            )

            val responseFlow = when (intent) {
                is Intent.BookRelated -> bookChatHandler.handle(
                    question = question,
                    route = intent,
                    history = filteredHistory,
                    book = book
                )

                is Intent.General -> generalChatHandler.handle(
                    question = question,
                    history = filteredHistory,
                    book = book
                )
            }

            responseFlow.collect { response ->
                emit(response)
                if (response is AiraResponse.Success) {
                    // Save User message
                    airaMessageDao.insertMessage(
                        AiraMessageEntity(
                            bookId = bookId,
                            role = AiraMessageRole.USER,
                            content = question.trim()
                        )
                    )
                    // Save Aira message
                    airaMessageDao.insertMessage(
                        AiraMessageEntity(
                            bookId = bookId,
                            role = AiraMessageRole.AIRA,
                            content = response.text.trim()
                        )
                    )
                }
            }

        } catch (e: Exception) {
            emit(AiraResponse.Error(resolveErrorMessage(e)))
        }
    }

    private fun resolveErrorMessage(e: Exception): String = when {
        e.message?.contains("MAX_TOKENS") == true ->
            "The answer was too long. Please try asking for a summary."

        e.message?.contains("resource-exhausted") == true ->
            "You've reached your free limit. Upgrade to Quill Pro."

        else -> "Something went wrong. Please try again."
    }
}
