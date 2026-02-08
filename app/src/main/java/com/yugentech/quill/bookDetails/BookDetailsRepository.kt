package com.yugentech.quill.bookDetails

import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.room.entities.BookDetailsEntity
import com.yugentech.quill.room.entities.LibraryBookEntity
import com.yugentech.quill.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow

interface BookDetailsRepository {

    // --- Flow-based queries for real-time updates ---
    fun getDetails(bookId: String): Flow<BookDetailsEntity?>
    fun getLibraryBook(bookId: String): Flow<LibraryBookEntity?>
    fun getAllCategories(): Flow<List<UserCategoryEntity>>

    // --- Snapshot queries for immediate state ---
    suspend fun getDetailsSnapshot(bookId: String): BookDetailsEntity?
    suspend fun getLibraryBookSnapshot(bookId: String): LibraryBookEntity?

    // --- Download management ---
    suspend fun startDownload(book: Book)
    suspend fun removeDownload(bookId: String)

    // --- Category management ---
    suspend fun updateCategory(bookId: String, newCategory: String)

    // --- Favorite management ---
    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean)
}