package com.yugentech.quill.reader.repository

import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import timber.log.Timber

class ReaderPreferencesRepositoryImpl(
    private val readerDataStore: ReaderDataStore
) : ReaderPreferencesRepository {

    // 1. Instantiate Readium's official JSON Serializer
    private val serializer = EpubPreferencesSerializer()

    override val readerPreferences: Flow<EpubPreferences> =
        readerDataStore.preferencesJsonFlow.map { jsonString ->
            if (jsonString != null) {
                try {
                    // 2. Use the serializer to decode the raw string
                    serializer.deserialize(jsonString)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse saved EpubPreferences")
                    ReaderDefaults.getPreferences()
                }
            } else {
                // First time opening the app, provide the default settings
                ReaderDefaults.getPreferences()
            }
        }

    override suspend fun savePreferences(preferences: EpubPreferences) {
        try {
            // 3. Use the serializer to encode the object back into a string
            val jsonString = serializer.serialize(preferences)
            readerDataStore.savePreferencesJson(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize EpubPreferences for saving")
        }
    }
}