package com.yugentech.quill.reader.pref.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val EPUB_PREFS_KEY = stringPreferencesKey("epub_prefs_json")
        private val VOLUME_NAV_KEY = booleanPreferencesKey("volume_navigation")
        private val NIGHT_LIGHT_KEY = booleanPreferencesKey("night_light")
    }

    val preferencesJsonFlow: Flow<String?> = dataStore.data.map { it[EPUB_PREFS_KEY] }
    val volumeNavFlow: Flow<Boolean> = dataStore.data.map { it[VOLUME_NAV_KEY] ?: false }
    val nightLightFlow: Flow<Boolean> = dataStore.data.map { it[NIGHT_LIGHT_KEY] ?: false }

    suspend fun savePreferencesJson(jsonString: String) {
        dataStore.edit { prefs -> prefs[EPUB_PREFS_KEY] = jsonString }
    }

    suspend fun saveVolumeNav(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[VOLUME_NAV_KEY] = enabled }
    }

    suspend fun saveNightLight(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NIGHT_LIGHT_KEY] = enabled }
    }
}