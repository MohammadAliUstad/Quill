package com.yugentech.quill.user.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.yugentech.quill.domain.model.SettingsConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

// The reminder settings share this DataStore but are read and written by NotificationDataStore.
class UserDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
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
                hapticsEnabled = prefs[HAPTICS_ENABLED] ?: true
            )
        }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }
}
