package com.yugentech.quill.reader.pref.repository

import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.epub.EpubPreferences

interface ReaderPrefRepository {
    val readerPreferences: Flow<EpubPreferences>
    suspend fun savePreferences(preferences: EpubPreferences)
}