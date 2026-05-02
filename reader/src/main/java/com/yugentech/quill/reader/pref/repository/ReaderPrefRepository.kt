package com.yugentech.quill.reader.pref.repository

import com.yugentech.quill.reader.pref.model.QuillPreferences
import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.epub.EpubPreferences

interface ReaderPrefRepository {
    val quillPreferences: Flow<QuillPreferences>
    suspend fun saveEpubPreferences(preferences: EpubPreferences)
    suspend fun saveVolumeNavigation(enabled: Boolean)
    suspend fun saveNightLight(enabled: Boolean)
}