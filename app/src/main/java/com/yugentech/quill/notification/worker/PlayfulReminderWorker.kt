package com.yugentech.quill.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.notification.NotificationHelper
import com.yugentech.quill.user.datastore.UserDataStore
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PlayfulReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val userDataStore: UserDataStore by inject()
    private val bookDao: BookDao by inject()
    private val notificationHelper: NotificationHelper by inject()

    override suspend fun doWork(): Result {
        val config = userDataStore.settingsConfiguration.first().notificationConfig
        
        Timber.d("PlayfulReminderWorker: Running work. Config: $config")

        if (!config.notificationsEnabled || !config.playfulRemindersEnabled) {
            Timber.d("PlayfulReminderWorker: Notifications or PlayfulReminders disabled. Skipping.")
            return Result.success()
        }

        val allBooks = bookDao.getAllBooksFlow().first()
        val lastReadBook = allBooks
            .filter { it.lastReadTime > 0 }
            .maxByOrNull { it.lastReadTime }
            
        val currentTime = System.currentTimeMillis()

        // 1. Check for long-term inactivity (6+ days)
        if (lastReadBook != null) {
            val millisSinceLast = currentTime - lastReadBook.lastReadTime
            val daysSinceLast = TimeUnit.MILLISECONDS.toDays(millisSinceLast)
            
            if (daysSinceLast >= 6) {
                // Trigger once every 2 days during inactivity
                val hoursSinceLast = TimeUnit.MILLISECONDS.toHours(millisSinceLast)
                val isCorrectDay = daysSinceLast % 2 == 0L
                val isFirstHalfOfDay = (hoursSinceLast % 24) < 12 
                
                if (isCorrectDay && isFirstHalfOfDay) {
                    sendInactivityNotification()
                }
                return Result.success()
            }
        }

        // 2. Random playful nudge (30% chance)
        if (Random.nextInt(100) < 30) {
            val bookToMention = if (lastReadBook != null && Random.nextBoolean()) {
                lastReadBook.title
            } else {
                allBooks.filter { it.downloadStatus.name == "DOWNLOADED" }
                    .randomOrNull()?.title ?: "your next book"
            }
            
            sendPlayfulNotification(bookToMention)
        }

        return Result.success()
    }

    private fun sendInactivityNotification() {
        val messages = listOf(
            "It's been a while! Aira is missing our reading sessions.",
            "Your reading streak is waiting for you. Shall we dive back in?",
            "Don't let your progress slip away. A few pages today?",
            "Reading is a journey, and we miss traveling with you. Ready to continue?"
        )
        notificationHelper.showReminderNotification(messages.random())
    }

    private fun sendPlayfulNotification(bookTitle: String) {
        val messages = listOf(
            "Ready to tackle '$bookTitle' again? You've got this!",
            "Psst... '$bookTitle' is calling your name!",
            "How about a quick chapter of '$bookTitle' before the day ends?",
            "Remember how good '$bookTitle' was getting? Let's find out what happens next.",
            "Just 10 minutes with '$bookTitle'? Your future self will thank you.",
            "Aira has some new insights about '$bookTitle'. Want to chat?",
            "Mastering '$bookTitle' one page at a time. Shall we?",
            "Don't let '$bookTitle' be lonely today!",
            "Reading time! '$bookTitle' is waiting for you.",
            "A quiet moment and '$bookTitle'. Perfection. Ready?"
        )
        notificationHelper.showReminderNotification(messages.random())
    }
}
