package com.yugentech.quill.insghts

import com.yugentech.quill.database.dao.BookQuestionCount
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

interface InsightsRepository {
    fun getAllSessionsFlow(): Flow<List<ReadingSessionEntity>>
    fun getStreakFlow(): Flow<Int>
    fun getAllBooksFlow(): Flow<List<BookEntity>>
    fun getFinishedBooksCountFlow(): Flow<Int>
    fun getTotalUserQuestionsFlow(): Flow<Int>
    fun getQuestionsPerBookFlow(): Flow<List<BookQuestionCount>>
    suspend fun getBookTitle(bookId: String): String
}