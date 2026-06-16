package com.yugentech.quill.aira.chat.bookChat.repository

import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface BookChatRepository {
    fun handle(
        question: String,
        route: Intent.BookRelated,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse>
}