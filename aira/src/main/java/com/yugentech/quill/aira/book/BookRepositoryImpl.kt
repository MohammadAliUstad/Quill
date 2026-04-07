package com.yugentech.quill.aira.book

import androidx.work.WorkManager
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val indexingStateDao: BookIndexingStateDao,
    private val workManager: WorkManager
) : BookRepository {

    override suspend fun getBookDetails(bookId: String): BookEntity? {
        return bookDao.getBookEntity(bookId)
    }

    override suspend fun observeIsReady(bookId: String): Flow<Boolean> {
        return indexingStateDao.observeIsComplete(bookId)
    }

    override suspend fun isReady(bookId: String): Boolean {
        return chunkDao.isBookIndexed(bookId)
    }

    override suspend fun isSpoilerLockEnabled(bookId: String): Boolean {
        return bookDao.getBookEntity(bookId)?.spoilerLockEnabled ?: true
    }

    override suspend fun setSpoilerLock(bookId: String, enabled: Boolean) {
        bookDao.updateSpoilerLock(bookId, enabled)
    }
}