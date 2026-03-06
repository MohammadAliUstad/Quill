package com.yugentech.quill.reader.repository

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
}

data class ReaderBookData(
    val localFilePath: String?,
    val totalPages: Int,
    val lastLocatorJson: String?
)