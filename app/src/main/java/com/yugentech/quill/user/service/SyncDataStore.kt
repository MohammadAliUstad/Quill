package com.yugentech.quill.user.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class SyncDataStore(
    private val dataStore: DataStore<Preferences>
) {
    private val userFetchDoneKey = booleanPreferencesKey("user_fetch_done")

    val isUserFetchDone: Flow<Boolean> = dataStore.data
        .catch {
            Timber.e(it, "Error reading user fetch sync flag")
            emit(emptyPreferences())
        }
        .map { prefs -> prefs[userFetchDoneKey] ?: false }

    suspend fun setUserFetchDone(done: Boolean) {
        Timber.d("Setting user fetch done: $done")
        dataStore.edit { it[userFetchDoneKey] = done }
    }

    suspend fun clearSyncFlags() {
        Timber.d("Clearing sync flags due to logout")
        dataStore.edit { it[userFetchDoneKey] = false }
    }
}