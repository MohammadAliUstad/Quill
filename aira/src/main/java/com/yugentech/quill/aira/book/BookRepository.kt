package com.yugentech.quill.aira.book

import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    suspend fun getBookDetails(bookId: String): BookEntity?
    suspend fun observeIsReady(bookId: String): Flow<Boolean>
    suspend fun isReady(bookId: String): Boolean
    suspend fun isSpoilerLockEnabled(bookId: String): Boolean
    suspend fun setSpoilerLock(bookId: String, enabled: Boolean)
    suspend fun indexLibraryBacklog()
}