package com.yugentech.quill.notification.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yugentech.quill.R

/** Rings the daily reminder. NudgeWorker posts the nudges through [showReminder] directly. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        showReminder(
            context = context,
            id = id,
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Quill",
            message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        )
        // Books tomorrow's as today's rings, from the latest saved time.
        ReminderStore.allDaily(context).firstOrNull { it.id == id }?.let { daily ->
            NotificationScheduler(context)
                .scheduleDailyReminder(daily.id, daily.hour, daily.minute, daily.title, daily.message)
        }
    }

    companion object {
        /** The reading reminder the user set: on time, with the default sound. */
        const val DAILY_CHANNEL_ID = "reading_reminders"

        /** Occasional nudges: silent and never heads-up, so they can't feel like nagging. */
        const val NUDGE_CHANNEL_ID = "nudges"

        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"

        /** Posts the reminder, in the nudge channel when [nudge]. */
        fun showReminder(
            context: Context,
            id: Int,
            title: String,
            message: String,
            nudge: Boolean = false
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        DAILY_CHANNEL_ID,
                        "Reading reminder",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Your daily reminder to read, at the time you choose"
                    }
                )
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        NUDGE_CHANNEL_ID,
                        "Friendly nudges",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Friendly nudges now and then, never more than one a day"
                    }
                )
            }

            // Tapping the reminder opens the app.
            val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
                PendingIntent.getActivity(
                    context,
                    id,
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val builder = NotificationCompat.Builder(context, if (nudge) NUDGE_CHANNEL_ID else DAILY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (nudge) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openApp)
                .setAutoCancel(true)

            notificationManager.notify(id, builder.build())
        }
    }
}
