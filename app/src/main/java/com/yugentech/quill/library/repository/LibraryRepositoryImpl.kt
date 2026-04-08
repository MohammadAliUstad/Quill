package com.yugentech.quill.library.repository

import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class LibraryRepositoryImpl(
    private val bookDao: BookDao
) : LibraryRepository {

    override fun getLastReadBook(): Flow<LibraryBookView?> {
        return bookDao.getLastReadBook()
    }

    override fun getReadingHistory(): Flow<List<LibraryBookView>> {
        return bookDao.getReadingHistory().map { list ->
            if (list.isNotEmpty()) list.drop(1) else emptyList()
        }
    }

    override fun getCompleteReadingHistory(): Flow<List<LibraryBookView>> {
        return bookDao.getReadingHistory()
    }

    override fun getBooksByCategory(category: String): Flow<List<LibraryBookView>> {
        return bookDao.getBooksByCategory(category)
    }

    override fun getBookShelf(): Flow<List<LibraryBookView>> {
        return bookDao.getBookShelf()
    }

    override fun getFavoriteBooks(): Flow<List<LibraryBookView>> {
        return bookDao.getFavoriteBooks()
    }

    override suspend fun getBook(bookId: String): BookEntity? {
        return bookDao.getBookEntity(bookId)
    }

    override suspend fun deleteBook(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        bookDao.removeDownload(bookId)
    }
}