package com.yugentech.quill.reader.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val EPUB_PREFS_KEY = stringPreferencesKey("epub_prefs_json")
    }

    // Exposes the raw JSON string
    val preferencesJsonFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[EPUB_PREFS_KEY]
    }

    // Saves the raw JSON string
    suspend fun savePreferencesJson(jsonString: String) {
        dataStore.edit { prefs ->
            prefs[EPUB_PREFS_KEY] = jsonString
        }
    }
}