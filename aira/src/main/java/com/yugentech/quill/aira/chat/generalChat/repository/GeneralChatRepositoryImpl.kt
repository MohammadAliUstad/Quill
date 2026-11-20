package com.yugentech.quill.aira.chat.generalChat.repository

import com.yugentech.quill.aira.chat.generalChat.model.GeneralChatPayload
import com.yugentech.quill.aira.chat.generalChat.service.GeneralChatService
import com.yugentech.quill.aira.util.AiraErrors
import com.yugentech.quill.aira.util.AiraLog
import com.yugentech.quill.aira.util.ChatUtils
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import com.yugentech.quill.aira.localization.AiraStrings
import com.yugentech.quill.aira.R

class GeneralChatRepositoryImpl(
    private val chatService: GeneralChatService
) : GeneralChatRepository {

    override fun handle(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity,
        selectedText: String?,
        userName: String?
    ): Flow<AiraResponse> = flow {
        val payload =
            GeneralChatPayload(
                query = question,
                bookTitle = book.title,
                bookAuthor = book.author,
                history = ChatUtils.formatHistory(history),
                selectedText = selectedText,
                userName = userName
            )

        try {
            val rawResponse = chatService.getChatResponse(payload)
            Timber.d("GeneralChat question=\"$question\" response: $rawResponse")
            emit(AiraResponse.Success(text = rawResponse))
        } catch (e: Exception) {
            AiraLog.failure("GeneralChat question=\"$question\"", e)
            val errorMsg = when {
                AiraErrors.isContentBlocked(e) -> AiraStrings.get(R.string.airachat_blocked)
                e.message?.contains("resource-exhausted") == true ->
                    AiraStrings.get(R.string.airachat_limit)
                else -> AiraStrings.get(R.string.airachat_error)
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }
}