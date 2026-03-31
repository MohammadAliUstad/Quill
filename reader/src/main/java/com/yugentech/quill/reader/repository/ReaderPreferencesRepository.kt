package com.yugentech.quill.reader.repository

import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.epub.EpubPreferences

interface ReaderPreferencesRepository {
    val readerPreferences: Flow<EpubPreferences>
    suspend fun savePreferences(preferences: EpubPreferences)
}