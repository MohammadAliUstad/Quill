package com.yugentech.quill.notification.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.yugentech.quill.notification.model.NotificationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** The saved reminder settings, the same in Quill and Sessions. */
class NotificationDataStore(
    private val dataStore: DataStore<Preferences>
) {

    val notificationConfigFlow: Flow<NotificationConfig> = dataStore.data
        .catch {
            Timber.e(it, "Error reading notification settings")
            emit(emptyPreferences())
        }
        .map { prefs ->
            NotificationConfig(
                notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
                dailyReminderEnabled = prefs[DAILY_REMINDER_ENABLED] ?: false,
                playfulRemindersEnabled = prefs[PLAYFUL_REMINDERS_ENABLED] ?: false,
                reminderHour = prefs[REMINDER_HOUR] ?: DEFAULT_REMINDER_HOUR,
                reminderMinute = prefs[REMINDER_MINUTE] ?: 0
            )
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[DAILY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setPlayfulRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[PLAYFUL_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[REMINDER_HOUR] = hour
            it[REMINDER_MINUTE] = minute
        }
    }

    private companion object {
        // In the "user" DataStore, under the names they've always had, so settings carry over.
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("reading_reminders_enabled")
        val PLAYFUL_REMINDERS_ENABLED = booleanPreferencesKey("playful_reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")

        const val DEFAULT_REMINDER_HOUR = 20
    }
}
