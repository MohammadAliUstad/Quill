package com.yugentech.quill.reader.settings.repository

import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.quill.reader.settings.model.ReaderSettings
import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.epub.EpubPreferences

interface ReaderSettingsRepository {
    val readerSettings: Flow<ReaderSettings>
    suspend fun saveEpubPreferences(preferences: EpubPreferences)
    suspend fun saveVolumeNavigation(enabled: Boolean)
    suspend fun saveNightLight(enabled: Boolean)
    suspend fun saveAutoPlaySound(enabled: Boolean)
    suspend fun saveLastSelectedSound(sound: BackgroundSound)
    suspend fun saveSoundVolume(volume: Float)
}
