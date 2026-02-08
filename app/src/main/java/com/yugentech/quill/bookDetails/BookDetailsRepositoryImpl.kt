package com.yugentech.quill.bookDetails

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.room.BookMappers
import com.yugentech.quill.room.daos.BookDetailsDao
import com.yugentech.quill.room.daos.CategoryDao
import com.yugentech.quill.room.daos.LibraryBooksDao
import com.yugentech.quill.room.entities.BookDetailsEntity
import com.yugentech.quill.room.entities.DownloadStatus
import com.yugentech.quill.room.entities.LibraryBookEntity
import com.yugentech.quill.room.entities.UserCategoryEntity
import com.yugentech.quill.workmanager.BookDownloadWorker
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookDetailsRepositoryImpl(
    private val bookDetailsDao: BookDetailsDao,
    private val libraryDao: LibraryBooksDao,
    private val categoryDao: CategoryDao,
    private val workManager: WorkManager
) : BookDetailsRepository {

    // --- Flow-based queries for real-time updates ---

    override fun getDetails(bookId: String): Flow<BookDetailsEntity?> {
        return bookDetailsDao.getDetailsFlow(bookId)
    }

    override fun getLibraryBook(bookId: String): Flow<LibraryBookEntity?> {
        return libraryDao.getBookByIdFlow(bookId)
    }

    override fun getAllCategories(): Flow<List<UserCategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    // --- Snapshot queries for immediate state ---

    override suspend fun getDetailsSnapshot(bookId: String): BookDetailsEntity? {
        return bookDetailsDao.getDetails(bookId)
    }

    override suspend fun getLibraryBookSnapshot(bookId: String): LibraryBookEntity? {
        return libraryDao.getBookById(bookId)
    }

    // --- Download management ---

    override suspend fun startDownload(book: Book) {
        // 1. Save "Lite" entity (So it shows up in Library with Spinner)
        val libraryBookEntity = BookMappers.toLibraryEntity(book).copy(
            downloadStatus = DownloadStatus.DOWNLOADING,
            addedAt = System.currentTimeMillis()
        )
        libraryDao.addToLibrary(libraryBookEntity)

        // 2. Save "Heavy" entity (Description, Subjects)
        // The Worker needs this to exist so it can find it and update the file path later
        val heavyEntity = BookMappers.toBookDetailsEntity(book)
        bookDetailsDao.insertDetails(heavyEntity)

        // 3. Queue the Worker
        val workRequest = OneTimeWorkRequestBuilder<BookDownloadWorker>()
            .setInputData(
                workDataOf(
                    "BOOK_ID" to book.id,
                    "DOWNLOAD_URL" to book.downloadUrl,
                    "BOOK_TITLE" to book.title
                )
            )
            .addTag("download_${book.id}")
            .build()

        workManager.enqueue(workRequest)
    }

    override suspend fun removeDownload(bookId: String) {
        val details = bookDetailsDao.getDetails(bookId)

        // Delete the physical file if it exists
        details?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        // Remove the file path from database (soft delete)
        bookDetailsDao.removeLocalFile(bookId)

        // Update download status
        libraryDao.updateDownloadStatus(bookId, DownloadStatus.NOT_DOWNLOADED)
    }

    // --- Category management ---

    override suspend fun updateCategory(bookId: String, newCategory: String) {
        libraryDao.moveBookToCategory(bookId, newCategory)
    }

    // --- Favorite management ---

    override suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        libraryDao.updateFavoriteStatus(bookId, isFavorite)
    }
}