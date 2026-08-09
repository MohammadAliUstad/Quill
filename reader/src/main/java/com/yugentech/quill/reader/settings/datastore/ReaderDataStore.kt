package com.yugentech.quill.reader.settings.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        private val AUTO_PLAY_SOUND_KEY = booleanPreferencesKey("auto_play_sound")
        private val LAST_SELECTED_SOUND_KEY = stringPreferencesKey("last_selected_sound")
        private val SOUND_VOLUME_KEY = floatPreferencesKey("sound_volume")
    }

    val preferencesJsonFlow: Flow<String?> = dataStore.data.map { it[EPUB_PREFS_KEY] }
    val volumeNavFlow: Flow<Boolean> = dataStore.data.map { it[VOLUME_NAV_KEY] ?: false }
    val nightLightFlow: Flow<Boolean> = dataStore.data.map { it[NIGHT_LIGHT_KEY] ?: false }
    val autoPlaySoundFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY_SOUND_KEY] ?: false }
    val lastSelectedSoundFlow: Flow<String?> = dataStore.data.map { it[LAST_SELECTED_SOUND_KEY] }
    val soundVolumeFlow: Flow<Float> = dataStore.data.map { it[SOUND_VOLUME_KEY] ?: 1.0f }

    suspend fun savePreferencesJson(jsonString: String) {
        dataStore.edit { prefs -> prefs[EPUB_PREFS_KEY] = jsonString }
    }

    suspend fun saveVolumeNav(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[VOLUME_NAV_KEY] = enabled }
    }

    suspend fun saveNightLight(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NIGHT_LIGHT_KEY] = enabled }
    }

    suspend fun saveAutoPlaySound(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_PLAY_SOUND_KEY] = enabled }
    }

    suspend fun saveLastSelectedSound(soundId: String) {
        dataStore.edit { prefs -> prefs[LAST_SELECTED_SOUND_KEY] = soundId }
    }

    suspend fun saveSoundVolume(volume: Float) {
        dataStore.edit { prefs -> prefs[SOUND_VOLUME_KEY] = volume }
    }
}