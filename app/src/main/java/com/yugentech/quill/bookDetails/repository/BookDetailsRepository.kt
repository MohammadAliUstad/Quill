package com.yugentech.quill.bookDetails.repository

import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.model.Book
import kotlinx.coroutines.flow.Flow

interface BookDetailsRepository {
    fun getBook(bookId: String): Flow<BookEntity?>
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun isBookInLibrary(bookId: String): Boolean
    suspend fun getBookOnce(bookId: String): BookEntity?
    suspend fun startDownload(book: Book)
    suspend fun removeDownload(bookId: String)
    suspend fun deleteBook(bookId: String)
    suspend fun updateCategory(book: Book, newCategory: String)
    suspend fun updateFavorite(book: Book, isFavorite: Boolean)
    suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int)
    suspend fun resetReadingProgress(bookId: String)
    suspend fun removeFromRecent(bookId: String)

    // Aira
    /** Whether the book's index for Aira is finished. */
    fun observeIsReady(bookId: String): Flow<Boolean>
    suspend fun isSpoilerLockEnabled(bookId: String): Boolean
    suspend fun setSpoilerLock(bookId: String, enabled: Boolean)

    /**
     * Queues every downloaded book whose index isn't finished and isn't already queued: books
     * whose indexing failed, was cancelled, or was cut short pick up where they left off.
     */
    suspend fun indexUnfinishedBooks()
}