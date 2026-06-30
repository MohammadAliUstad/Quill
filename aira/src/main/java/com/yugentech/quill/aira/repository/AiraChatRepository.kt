package com.yugentech.quill.aira.repository

import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.aira.response.AiraResponse
import kotlinx.coroutines.flow.Flow

interface AiraChatRepository {
    suspend fun ask(bookId: String, query: String): Flow<AiraResponse>
    suspend fun getMessagesForBook(bookId: String): Flow<List<AiraMessage>>
    suspend fun clearMessagesForBook(bookId: String)
}