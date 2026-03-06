package com.yugentech.quill.aira.aira.repository

import com.yugentech.quill.aira.aira.AiraMessage
import com.yugentech.quill.aira.aira.AiraResponse
import kotlinx.coroutines.flow.Flow

interface AiraRepository {
    suspend fun isReady(bookId: String): Boolean
    suspend fun isSpoilerLockEnabled(bookId: String): Boolean
    suspend fun setSpoilerLock(bookId: String, enabled: Boolean)
    fun getMessagesForBook(bookId: String): Flow<List<AiraMessage>>
    suspend fun ask(bookId: String, question: String): Flow<AiraResponse>
}