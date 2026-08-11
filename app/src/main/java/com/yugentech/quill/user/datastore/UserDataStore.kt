package com.yugentech.quill.user.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.yugentech.quill.domain.model.NotificationConfig
import com.yugentech.quill.domain.model.SettingsConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class UserDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val READING_REMINDERS_ENABLED = booleanPreferencesKey("reading_reminders_enabled")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val PLAYFUL_REMINDERS_ENABLED = booleanPreferencesKey("playful_reminders_enabled")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    val settingsConfiguration: Flow<SettingsConfiguration> = dataStore.data
        .catch {
            Timber.e(it, "Error reading settings preferences")
            emit(emptyPreferences())
        }
        .map { prefs ->
            SettingsConfiguration(
                hapticsEnabled = prefs[HAPTICS_ENABLED] ?: true,
                notificationConfig = NotificationConfig(
                    notificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true,
                    readingRemindersEnabled = prefs[READING_REMINDERS_ENABLED] ?: false,
                    playfulRemindersEnabled = prefs[PLAYFUL_REMINDERS_ENABLED] ?: false,
                    reminderTimeHour = prefs[REMINDER_HOUR] ?: 20,
                    reminderTimeMinute = prefs[REMINDER_MINUTE] ?: 0
                )
            )
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReadingRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[READING_REMINDERS_ENABLED] = enabled }
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

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }
}