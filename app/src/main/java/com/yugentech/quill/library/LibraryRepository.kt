package com.yugentech.quill.library

import com.yugentech.quill.room.entities.LibraryBookEntity
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    // Existing methods
    fun getBooks(sortOption: SortOption): Flow<List<LibraryBookEntity>>
    suspend fun getBook(bookId: String): LibraryBookEntity?
    fun getBookFlow(bookId: String): Flow<LibraryBookEntity?>
    suspend fun deleteBook(bookId: String)
    suspend fun updateProgress(bookId: String, chapter: Int, scroll: Int, percent: Float)

    // NEW: Category-based queries
    fun getBooksByCategory(category: String, sortOption: SortOption): Flow<List<LibraryBookEntity>>

    // NEW: Special sections
    fun getRecentBooks(limit: Int = 20): Flow<List<LibraryBookEntity>>
    fun getFavoriteBooks(): Flow<List<LibraryBookEntity>>
    fun getUncategorizedBooks(): Flow<List<LibraryBookEntity>>

    // NEW: Book management
    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean)
    suspend fun moveToCategory(bookId: String, category: String)
}