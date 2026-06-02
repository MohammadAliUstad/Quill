package com.yugentech.quill.aira.aira.generalChat.handler

import com.yugentech.quill.aira.aira.generalChat.model.GeneralChatPayload
import com.yugentech.quill.aira.aira.generalChat.service.GeneralChatService
import com.yugentech.quill.aira.aira.chat.util.ChatUtils
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeneralChatHandler(
    private val chatService: GeneralChatService
) {
    fun handle(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        val payload = GeneralChatPayload(
            prompt = question,
            bookTitle = book.title,
            bookAuthor = book.author,
            history = ChatUtils.formatHistory(history)
        )

        val rawResponse = chatService.getChatResponse(payload)
        emit(AiraResponse.Success(text = rawResponse))
    }
}
