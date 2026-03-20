package com.yugentech.quill.reader.repository

import com.yugentech.quill.database.dao.ReadingSessionDao
import com.yugentech.quill.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

class ReadingSessionRepositoryImpl(
    private val dao: ReadingSessionDao
) : ReadingSessionRepository {

    override suspend fun insertSession(bookId: String, startTime: Long, endTime: Long) {
        val duration = endTime - startTime
        val session = ReadingSessionEntity(
            bookId = bookId,
            startTime = startTime,
            endTime = endTime,
            durationMillis = duration
        )
        dao.insertSession(session)
    }

    override fun getAllSessionsFlow(): Flow<List<ReadingSessionEntity>> {
        return dao.getAllSessionsFlow()
    }

    override fun getSessionsForBookFlow(bookId: String): Flow<List<ReadingSessionEntity>> {
        return dao.getSessionsForBookFlow(bookId)
    }

    override suspend fun deleteSessionsForBook(bookId: String) {
        dao.deleteSessionsForBook(bookId)
    }
}