package com.yugentech.quill.reader.pref.repository

import com.yugentech.quill.reader.pref.datastore.ReaderDataStore
import com.yugentech.quill.reader.pref.model.QuillPreferences
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import timber.log.Timber

class ReaderPrefRepositoryImpl(
    private val readerDataStore: ReaderDataStore
) : ReaderPrefRepository {

    private val serializer = EpubPreferencesSerializer()

    override val quillPreferences: Flow<QuillPreferences> = combine(
        readerDataStore.preferencesJsonFlow,
        readerDataStore.volumeNavFlow,
        readerDataStore.nightLightFlow
    ) { jsonString, volumeNav, nightLight ->
        val epub = if (jsonString != null) {
            try {
                serializer.deserialize(jsonString)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse saved EpubPreferences")
                ReaderDefaults.getPreferences()
            }
        } else {
            ReaderDefaults.getPreferences()
        }
        QuillPreferences(epub, volumeNav, nightLight)
    }

    override suspend fun saveEpubPreferences(preferences: EpubPreferences) {
        try {
            val jsonString = serializer.serialize(preferences)
            readerDataStore.savePreferencesJson(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize EpubPreferences for saving")
        }
    }

    override suspend fun saveVolumeNavigation(enabled: Boolean) {
        readerDataStore.saveVolumeNav(enabled)
    }

    override suspend fun saveNightLight(enabled: Boolean) {
        readerDataStore.saveNightLight(enabled)
    }
}