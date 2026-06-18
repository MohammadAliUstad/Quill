package com.yugentech.quill.aira.chat.generalChat.repository

import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface GeneralChatRepository {
    fun handle(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse>
}