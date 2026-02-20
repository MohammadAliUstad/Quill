package com.yugentech.quill.reader

import com.yugentech.quill.room.entities.BookEntity
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    fun getBook(bookId: String): Flow<BookEntity?>
    suspend fun saveProgress(
        bookId: String,
        progress: Float,
        chapterTitle: String?,
        chapterIndex: Int,
        locatorJson: String?
    )
}