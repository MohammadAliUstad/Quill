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
    suspend fun startDownload(book: Book, isPro: Boolean)
    suspend fun removeDownload(bookId: String)
    suspend fun deleteBook(bookId: String)
    suspend fun updateCategory(book: Book, newCategory: String)
    suspend fun updateFavorite(book: Book, isFavorite: Boolean)
    suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int)
    suspend fun resetReadingProgress(bookId: String)
    suspend fun removeFromRecent(bookId: String)
}