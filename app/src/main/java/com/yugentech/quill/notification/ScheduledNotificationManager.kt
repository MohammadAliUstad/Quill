package com.yugentech.quill.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yugentech.quill.notification.worker.PlayfulReminderWorker
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ScheduledNotificationManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val DAILY_REMINDER_REQUEST_CODE = 2000
        private const val PLAYFUL_REMINDERS_WORK_NAME = "playful_reminders_work"
    }

    fun scheduleReminder(hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Timber.e("Attempted to schedule exact alarm without permission")
                throw SecurityException("Exact alarm permission not granted")
            }
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Timber.i("Scheduling exact reading reminder for: ${calendar.time}")

        val intent = Intent(context, ScheduledReceiver::class.java).apply {
            putExtra(ScheduledReceiver.EXTRA_HOUR, hour)
            putExtra(ScheduledReceiver.EXTRA_MINUTE, minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelReminders() {
        Timber.i("Cancelling all scheduled reminders")
        val intent = Intent(context, ScheduledReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedulePlayfulReminders() {
        Timber.i("Scheduling playful reminders via WorkManager")
        val workRequest = PeriodicWorkRequestBuilder<PlayfulReminderWorker>(
            12, TimeUnit.HOURS
        ).setInitialDelay(6, TimeUnit.HOURS)
            .addTag(PLAYFUL_REMINDERS_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PLAYFUL_REMINDERS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelPlayfulReminders() {
        Timber.i("Cancelling playful reminders")
        WorkManager.getInstance(context).cancelUniqueWork(PLAYFUL_REMINDERS_WORK_NAME)
    }
}
