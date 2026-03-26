package com.yugentech.quill.reader.repository

import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderRepositoryImpl(
    private val bookDao: BookDao
) : ReaderRepository {

    override fun getBook(bookId: String): Flow<ReaderBookData?> =
        bookDao.getBookEntityFlow(bookId).map { entity ->
            entity?.let {
                ReaderBookData(
                    localFilePath = it.localFilePath,
                    totalPages = it.totalPages,
                    lastLocatorJson = it.lastLocatorJson
                )
            }
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