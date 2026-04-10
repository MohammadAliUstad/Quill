package com.yugentech.quill.reader.datastore

import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import timber.log.Timber

class ReaderPrefRepositoryImpl(
    private val readerDataStore: ReaderDataStore
) : ReaderPrefRepository {

    private val serializer = EpubPreferencesSerializer()

    override val readerPreferences: Flow<EpubPreferences> =
        readerDataStore.preferencesJsonFlow.map { jsonString ->
            if (jsonString != null) {
                try {
                    serializer.deserialize(jsonString)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse saved EpubPreferences")
                    ReaderDefaults.getPreferences()
                }
            } else {
                ReaderDefaults.getPreferences()
            }
        }

    override suspend fun savePreferences(preferences: EpubPreferences) {
        try {
            val jsonString = serializer.serialize(preferences)
            readerDataStore.savePreferencesJson(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize EpubPreferences for saving")
        }
    }
}