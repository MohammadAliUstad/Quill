package com.yugentech.quill.library.repository

import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

    // --- 1. SPECIAL SECTIONS ---
    fun getLastReadBook(): Flow<LibraryBookView?>
    fun getReadingHistory(): Flow<List<LibraryBookView>>
    fun getCompleteReadingHistory(): Flow<List<LibraryBookView>>

    // --- 2. BROWSING ---
    fun getBooksByCategory(category: String): Flow<List<LibraryBookView>>
    fun getBookShelf(): Flow<List<LibraryBookView>>
    fun getFavoriteBooks(): Flow<List<LibraryBookView>>

    // --- 3. DATA ACCESS ---
    suspend fun getBook(bookId: String): BookEntity?

    // --- 4. MANAGEMENT ---
    suspend fun deleteBook(bookId: String)
}