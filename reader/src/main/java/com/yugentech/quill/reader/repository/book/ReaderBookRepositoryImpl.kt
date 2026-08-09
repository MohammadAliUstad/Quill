package com.yugentech.quill.reader.repository.book

import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.HighlightDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReaderBookRepositoryImpl(
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val indexingStateDao: BookIndexingStateDao
) : ReaderBookRepository {

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

    override fun getHighlights(bookId: String): Flow<List<HighlightEntity>> {
        return highlightDao.getHighlightsForBookFlow(bookId)
    }

    override suspend fun saveHighlight(highlight: HighlightEntity) {
        highlightDao.insertHighlight(highlight)
    }

    override suspend fun deleteHighlight(highlightId: String) {
        highlightDao.deleteHighlight(highlightId)
    }

    override fun observeIsReady(bookId: String): Flow<Boolean> {
        return indexingStateDao.observeIsComplete(bookId)
    }
}