package com.yugentech.quill.aira.book

import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao
) : BookRepository {

    override suspend fun getBookDetails(bookId: String): BookEntity? {
        return bookDao.getBookEntity(bookId)
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