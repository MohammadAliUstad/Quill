package com.yugentech.quill.aira.book

import com.yugentech.quill.database.entity.BookEntity

interface BookRepository {
    suspend fun getBookDetails(bookId: String): BookEntity?
    suspend fun isReady(bookId: String): Boolean
    suspend fun isSpoilerLockEnabled(bookId: String): Boolean
    suspend fun setSpoilerLock(bookId: String, enabled: Boolean)
}