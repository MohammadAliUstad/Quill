package com.yugentech.quill.library

import com.yugentech.quill.room.daos.LibraryBooksDao
import com.yugentech.quill.room.entities.LibraryBookEntity
import kotlinx.coroutines.flow.Flow

class LibraryRepositoryImpl(
    private val libraryDao: LibraryBooksDao
) : LibraryRepository {

    // Existing methods
    override fun getBooks(sortOption: SortOption): Flow<List<LibraryBookEntity>> {
        return when (sortOption) {
            SortOption.RECENTLY_ADDED -> libraryDao.getBooksByDateAdded()
            SortOption.LAST_READ -> libraryDao.getBooksByLastRead()
            SortOption.TITLE -> libraryDao.getBooksByTitle()
            SortOption.AUTHOR -> libraryDao.getBooksByAuthor()
        }
    }

    override suspend fun getBook(bookId: String) = libraryDao.getBookById(bookId)

    override fun getBookFlow(bookId: String) = libraryDao.getBookByIdFlow(bookId)

    override suspend fun deleteBook(bookId: String) {
        libraryDao.removeFromLibrary(bookId)
    }

    override suspend fun updateProgress(bookId: String, chapter: Int, scroll: Int, percent: Float) {
        libraryDao.updateProgress(bookId, chapter, scroll, percent)
    }

    // NEW: Category-based queries
    override fun getBooksByCategory(category: String, sortOption: SortOption): Flow<List<LibraryBookEntity>> {
        return when (sortOption) {
            SortOption.RECENTLY_ADDED -> libraryDao.getBooksByCategoryDateAdded(category)
            SortOption.LAST_READ -> libraryDao.getBooksByCategoryLastRead(category)
            SortOption.TITLE -> libraryDao.getBooksByCategoryTitle(category)
            SortOption.AUTHOR -> libraryDao.getBooksByCategoryAuthor(category)
        }
    }

    // NEW: Special sections
    override fun getRecentBooks(limit: Int): Flow<List<LibraryBookEntity>> {
        return libraryDao.getRecentBooks(limit)
    }

    override fun getFavoriteBooks(): Flow<List<LibraryBookEntity>> {
        return libraryDao.getFavoriteBooks()
    }

    override fun getUncategorizedBooks(): Flow<List<LibraryBookEntity>> {
        return libraryDao.getUncategorizedBooks()
    }

    // NEW: Book management
    override suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        libraryDao.updateFavoriteStatus(bookId, isFavorite)
    }

    override suspend fun moveToCategory(bookId: String, category: String) {
        libraryDao.moveBookToCategory(bookId, category)
    }
}