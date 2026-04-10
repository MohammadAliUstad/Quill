package com.yugentech.quill.reader.session

import com.yugentech.quill.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

interface ReadingSessionRepository {
    suspend fun insertSession(bookId: String, startTime: Long, endTime: Long)
    fun getAllSessionsFlow(): Flow<List<ReadingSessionEntity>>
    fun getSessionsForBookFlow(bookId: String): Flow<List<ReadingSessionEntity>>
    suspend fun deleteSessionsForBook(bookId: String)
}