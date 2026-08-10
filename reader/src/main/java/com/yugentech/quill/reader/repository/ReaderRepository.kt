package com.yugentech.quill.reader.repository

import com.yugentech.quill.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    fun getBook(bookId: String): Flow<ReaderBookData?>
    suspend fun saveProgress(
        bookId: String,
        progress: Float,
        chapterTitle: String?,
        chapterIndex: Int,
        locatorJson: String?
    )
    fun getHighlights(bookId: String): Flow<List<HighlightEntity>>
    suspend fun saveHighlight(highlight: HighlightEntity)
    suspend fun deleteHighlight(highlightId: String)
    fun observeIsReady(bookId: String): Flow<Boolean>
}

data class ReaderBookData(
    val localFilePath: String?,
    val totalPages: Int,
    val lastLocatorJson: String?
)