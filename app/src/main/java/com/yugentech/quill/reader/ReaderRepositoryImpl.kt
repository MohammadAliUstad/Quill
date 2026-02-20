package com.yugentech.quill.reader.reader

import com.yugentech.quill.room.daos.BookDao
import com.yugentech.quill.room.entities.BookEntity
import kotlinx.coroutines.flow.Flow

class ReaderRepositoryImpl(
    private val bookDao: BookDao
) : ReaderRepository {

    override fun getBook(bookId: String): Flow<BookEntity?> {
        return bookDao.getBookEntityFlow(bookId)
    }

    override suspend fun saveProgress(
        bookId: String,
        progress: Float,
        chapterTitle: String?,
        chapterIndex: Int,
        locatorJson: String?
    ) {
        bookDao.updateReadingProgress(
            bookId = bookId,
            progress = progress,
            chapterTitle = chapterTitle,
            chapterIndex = chapterIndex,
            locatorJson = locatorJson,
            readTime = System.currentTimeMillis()
        )
    }
}