package com.yugentech.quill.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yugentech.quill.user.datastore.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ScheduledReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationHelper: NotificationHelper by inject()
    private val scheduledNotificationManager: ScheduledNotificationManager by inject()
    private val userDataStore: UserDataStore by inject()

    companion object {
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAfterBoot()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val hour = intent.getIntExtra(EXTRA_HOUR, 20)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        Timber.i("Reading reminder alarm fired. Showing notification.")

        notificationHelper.showReminderNotification("Aira is waiting for you! Let's dive back into your book.")

        if (scheduledNotificationManager.canScheduleExactAlarms()) {
            try {
                scheduledNotificationManager.scheduleReminder(hour, minute)
            } catch (e: SecurityException) {
                Timber.e(e, "Permission revoked, cannot reschedule next day alarm")
            }
        }
    }

    private suspend fun rescheduleAfterBoot() {
        if (scheduledNotificationManager.canScheduleExactAlarms()) {
            try {
                val config = userDataStore.settingsConfiguration.first().notificationConfig

                if (config.notificationsEnabled && config.readingRemindersEnabled) {
                    Timber.d("Boot completed. Rescheduling reminder for ${config.reminderTimeHour}:${config.reminderTimeMinute}")

                    scheduledNotificationManager.scheduleReminder(
                        hour = config.reminderTimeHour,
                        minute = config.reminderTimeMinute,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Boot reschedule failed")
            }
        }
    }
}
