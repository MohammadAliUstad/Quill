package com.yugentech.quill.reader.pref.repository

import com.yugentech.quill.reader.model.BackgroundSound
import com.yugentech.quill.reader.pref.datastore.ReaderDataStore
import com.yugentech.quill.reader.pref.model.QuillPreferences
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubPreferencesSerializer
import timber.log.Timber

class ReaderPrefRepositoryImpl(
    private val readerDataStore: ReaderDataStore
) : ReaderPrefRepository {

    private val serializer = EpubPreferencesSerializer()

    override val quillPreferences: Flow<QuillPreferences> = combine<Any?, QuillPreferences>(
        readerDataStore.preferencesJsonFlow,
        readerDataStore.volumeNavFlow,
        readerDataStore.nightLightFlow,
        readerDataStore.autoPlaySoundFlow,
        readerDataStore.lastSelectedSoundFlow,
        readerDataStore.soundVolumeFlow
    ) { args ->
        val jsonString = args[0] as? String
        val volumeNav = args[1] as Boolean
        val nightLight = args[2] as Boolean
        val autoPlay = args[3] as Boolean
        val lastSoundId = args[4] as? String
        val volume = args[5] as Float

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
        val lastSound = BackgroundSound.fromId(lastSoundId)
        QuillPreferences(epub, volumeNav, nightLight, autoPlay, lastSound, volume)
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

    override suspend fun saveAutoPlaySound(enabled: Boolean) {
        readerDataStore.saveAutoPlaySound(enabled)
    }

    override suspend fun saveLastSelectedSound(sound: BackgroundSound) {
        readerDataStore.saveLastSelectedSound(sound.id)
    }

    override suspend fun saveSoundVolume(volume: Float) {
        readerDataStore.saveSoundVolume(volume)
    }
}
