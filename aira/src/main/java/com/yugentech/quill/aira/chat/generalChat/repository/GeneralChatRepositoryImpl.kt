package com.yugentech.quill.aira.chat.generalChat.repository

import com.yugentech.quill.aira.chat.generalChat.model.GeneralChatPayload
import com.yugentech.quill.aira.chat.generalChat.service.GeneralChatService
import com.yugentech.quill.aira.util.ChatUtils
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

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
            val errorMsg = when {
                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."
                else -> "Something went wrong. Please try again."
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }
}