package com.yugentech.quill.aira.service

import com.yugentech.quill.aira.chat.bookChat.repository.BookChatRepository
import com.yugentech.quill.aira.chat.generalChat.repository.GeneralChatRepository
import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.intent.repository.IntentDetectionRepository
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class AiraChatService(
    private val intentDetectionRepository: IntentDetectionRepository,
    private val generalChatRepository: GeneralChatRepository,
    private val bookChatRepository: BookChatRepository,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao
) {

    fun ask(bookId: String, query: String): Flow<AiraResponse> = flow {
        Timber.d("AiraChatService: Starting ask flow for bookId=$bookId, query='$query'")
        val book = bookDao.getBookEntity(bookId)!!
        val recentHistory = airaMessageDao.getRecentMessagesForBook(bookId)
        Timber.d("AiraChatService: Fetched ${recentHistory.size} history messages")

        try {
            val intent = intentDetectionRepository.detectIntent(
                query = query,
                title = book.title,
                author = book.author
            )
            Timber.d("AiraChatService: Detected intent: $intent")

            val responseFlow = when (intent) {
                is Intent.BookRelated -> {
                    Timber.d("AiraChatService: Routing to BookChatRepository")
                    bookChatRepository.handle(
                        question = query,
                        route = intent,
                        history = recentHistory,
                        book = book
                    )
                }

                is Intent.General -> {
                    Timber.d("AiraChatService: Routing to GeneralChatRepository")
                    generalChatRepository.handle(
                        question = query,
                        history = recentHistory,
                        book = book
                    )
                }
            }

            responseFlow.collect { response ->
                Timber.d("AiraChatService: Received response emission: $response")
                emit(response)
                if (response is AiraResponse.Success) {
                    Timber.d("AiraChatService: Saving turn to database")
                    // Save User message
                    airaMessageDao.insertMessage(
                        AiraMessageEntity(
                            bookId = bookId,
                            role = AiraMessageRole.USER,
                            content = query.trim()
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
            Timber.e(e, "AiraChatService: Fatal error in chat flow")
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