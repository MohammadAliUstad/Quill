package com.yugentech.quill.insghts.repository

import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookQuestionCount
import com.yugentech.quill.database.dao.ReadingSessionDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

class InsightsRepositoryImpl(
    private val bookDao: BookDao,
    private val readingSessionDao: ReadingSessionDao,
    private val airaMessageDao: AiraMessageDao
) : InsightsRepository {

    override fun getAllSessionsFlow(): Flow<List<ReadingSessionEntity>> =
        readingSessionDao.getAllSessionsFlow()

    override fun getStreakFlow(): Flow<Int> = getAllSessionsFlow().map { sessions ->
        calculateStreak(sessions)
    }

    override fun getAllBooksFlow(): Flow<List<BookEntity>> =
        bookDao.getAllBooksFlow()

    override fun getFinishedBooksCountFlow(): Flow<Int> =
        bookDao.getFinishedBooksCountFlow()

    override fun getTotalUserQuestionsFlow(): Flow<Int> =
        airaMessageDao.getTotalUserQuestionsFlow()

    override fun getQuestionsPerBookFlow(): Flow<List<BookQuestionCount>> =
        airaMessageDao.getQuestionsPerBookFlow()

    override suspend fun getBookTitle(bookId: String): String {
        return bookDao.getBookEntity(bookId)?.title ?: "Unknown Book"
    }

    private fun calculateStreak(sessions: List<ReadingSessionEntity>): Int {
        if (sessions.isEmpty()) return 0

        val sessionDates = sessions.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        if (sessionDates.first() < yesterday) return 0

        var streak = 0
        var lastDate = if (sessionDates.first() == today) today
        else yesterday + TimeUnit.DAYS.toMillis(1)

        for (date in sessionDates) {
            if (lastDate - date <= TimeUnit.DAYS.toMillis(1)) {
                streak++
                lastDate = date
            } else break
        }

        return streak
    }
}