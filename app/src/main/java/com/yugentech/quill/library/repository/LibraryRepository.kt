package com.yugentech.quill.library.repository

import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getLastReadBook(): Flow<LibraryBookView?>
    fun getReadingHistory(): Flow<List<LibraryBookView>>
    fun getCompleteReadingHistory(): Flow<List<LibraryBookView>>
    fun getBooksByCategory(category: String): Flow<List<LibraryBookView>>
    fun getBookShelf(): Flow<List<LibraryBookView>>
    fun getFavoriteBooks(): Flow<List<LibraryBookView>>
    suspend fun getBook(bookId: String): BookEntity?
    suspend fun deleteBook(bookId: String)
}