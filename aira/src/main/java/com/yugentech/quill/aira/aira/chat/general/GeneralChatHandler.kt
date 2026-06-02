package com.yugentech.quill.aira.aira.chat.general

import com.yugentech.quill.aira.aira.chat.model.ChatPayload
import com.yugentech.quill.aira.aira.chat.model.ChatRequestType
import com.yugentech.quill.aira.aira.chat.service.AiraChatService
import com.yugentech.quill.aira.aira.chat.util.ChatUtils
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeneralChatHandler(
    private val chatService: AiraChatService
) {
    fun handle(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        val payload = ChatPayload(
            prompt = question,
            requestType = ChatRequestType.GENERAL,
            bookTitle = book.title,
            bookAuthor = book.author,
            history = ChatUtils.formatHistory(history)
        )

        val rawResponse = chatService.getChatResponse(payload)
        emit(AiraResponse.Success(text = rawResponse))
    }
}
