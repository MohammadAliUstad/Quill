package com.yugentech.quill.aira.service

import com.yugentech.quill.aira.chat.bookChat.repository.BookChatRepository
import com.yugentech.quill.aira.chat.generalChat.repository.GeneralChatRepository
import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.intent.repository.IntentDetectionRepository
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.R
import com.yugentech.quill.aira.localization.AiraStrings
import com.yugentech.quill.aira.util.AiraErrors
import com.yugentech.quill.aira.util.AiraLog
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.UserDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import com.yugentech.quill.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiraChatService(
    private val intentDetectionRepository: IntentDetectionRepository,
    private val generalChatRepository: GeneralChatRepository,
    private val bookChatRepository: BookChatRepository,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao,
    private val authRepository: AuthRepository,
    private val userDao: UserDao
) {

    fun ask(bookId: String, query: String, selectedText: String? = null): Flow<AiraResponse> = flow {
        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            AiraLog.w("Ask: book not found")
            emit(AiraResponse.Error(resolveErrorMessage(IllegalStateException("Book $bookId not found"))))
            return@flow
        }
        val recentHistory = airaMessageDao.getRecentMessagesForBook(bookId)
        val currentUserId = authRepository.currentUser
        val userName = currentUserId?.let { userId ->
            userDao.getUser(userId)?.name?.takeIf { it.isNotBlank() }
        } ?: authRepository.currentUserName

        // The latest question and Aira's reply to it, for resolving a follow-up's references
        val lastAnswerIndex = recentHistory.indexOfLast { it.role == AiraMessageRole.AIRA }
        val lastQuestion = recentHistory.take(lastAnswerIndex.coerceAtLeast(0))
            .lastOrNull { it.role == AiraMessageRole.USER }?.content
        val lastAnswer = recentHistory.getOrNull(lastAnswerIndex)?.content

        try {
            val intent = intentDetectionRepository.detectIntent(
                query = query,
                title = book.title,
                author = book.author,
                selectedText = selectedText,
                lastQuestion = lastQuestion,
                lastAnswer = lastAnswer
            )

            val responseFlow = when (intent) {
                is Intent.BookRelated -> {
                    bookChatRepository.handle(
                        question = query,
                        route = intent,
                        history = recentHistory,
                        book = book,
                        selectedText = selectedText,
                        userName = userName
                    )
                }

                is Intent.General -> {
                    generalChatRepository.handle(
                        question = query,
                        history = recentHistory,
                        book = book,
                        selectedText = selectedText,
                        userName = userName
                    )
                }
            }

            responseFlow.collect { response ->
                emit(response)
                if (response is AiraResponse.Success) {
                    airaMessageDao.insertMessages(
                        listOf(
                            AiraMessageEntity(
                                bookId = bookId,
                                role = AiraMessageRole.USER,
                                content = query.trim()
                            ),
                            AiraMessageEntity(
                                bookId = bookId,
                                role = AiraMessageRole.AIRA,
                                content = response.text.trim()
                            )
                        )
                    )
                }
            }

        } catch (e: Exception) {
            AiraLog.failure("Ask", e)
            emit(AiraResponse.Error(resolveErrorMessage(e)))
        }
    }

    private fun resolveErrorMessage(e: Exception): String = when {
        AiraErrors.isContentBlocked(e) -> AiraStrings.get(R.string.airachat_blocked)

        e.message?.contains("MAX_TOKENS") == true ->
            "The answer was too long. Please try asking for a summary."

        e.message?.contains("resource-exhausted") == true ->
            "You've reached your free limit. Upgrade to Quill Pro."

        else -> "Something went wrong. Please try again."
    }
}