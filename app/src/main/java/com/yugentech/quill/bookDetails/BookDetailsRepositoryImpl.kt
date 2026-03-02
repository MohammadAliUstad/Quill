package com.yugentech.quill.bookDetails

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.room.BookMappers.toEntity
import com.yugentech.quill.room.daos.BookDao
import com.yugentech.quill.room.daos.CategoryDao
import com.yugentech.quill.room.entities.BookEntity
import com.yugentech.quill.room.entities.CategoryEntity
import com.yugentech.quill.room.entities.DownloadStatus
import com.yugentech.quill.workmanager.BookDownloadWorker
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookDetailsRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val workManager: WorkManager
) : BookDetailsRepository {

    // Reads
    override fun getBook(bookId: String): Flow<BookEntity?> {
        return bookDao.getBookEntityFlow(bookId)
    }

    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun isBookInLibrary(bookId: String): Boolean {
        return bookDao.hasBook(bookId)
    }

    override suspend fun getBookOnce(bookId: String): BookEntity? {
        return bookDao.getBookEntity(bookId)
    }

    // Actions
    override suspend fun startDownload(book: Book) {
        // 1. Check if we already have this book (to preserve Favorite/Category status & PROGRESS)
        val existingBook = bookDao.getBookEntity(book.id)

        // 2. Prepare the Entity
        val newEntity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            coverUrl = book.coverUrl,
            downloadUrl = book.downloadUrl,
            source = book.source,
            description = book.description,
            subjects = book.subjects,
            language = book.language,
            downloadStatus = DownloadStatus.DOWNLOADING,

            // --- PRESERVED METADATA ---
            isFavorite = existingBook?.isFavorite ?: false,
            userCategory = existingBook?.userCategory ?: "Shelf",
            addedAt = existingBook?.addedAt ?: System.currentTimeMillis(),

            // --- FIX: PRESERVED PROGRESS DATA ---
            progressPercent = existingBook?.progressPercent ?: 0f,
            totalPages = existingBook?.totalPages ?: 0,
            lastChapterTitle = existingBook?.lastChapterTitle,
            lastReadTime = existingBook?.lastReadTime ?: 0,
            lastChapterIndex = existingBook?.lastChapterIndex ?: 0,
            lastScrollPosition = existingBook?.lastScrollPosition ?: 0,
            lastLocatorJson = existingBook?.lastLocatorJson
        )

        // 3. Save to Database
        bookDao.insertBook(newEntity)

        // 4. Start the WorkManager Task
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
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        bookDao.removeDownload(bookId)
    }

    override suspend fun deleteBook(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        bookDao.deleteBook(bookId)
    }

    override suspend fun updateFavorite(book: Book, isFavorite: Boolean) {
        val existingBook = bookDao.getBookEntity(book.id)

        if (existingBook != null) {
            bookDao.updateFavorite(book.id, isFavorite)
        } else {
            val newEntity = book.toEntity(
                isFavorite = isFavorite,
                status = DownloadStatus.NOT_DOWNLOADED
            )
            bookDao.insertBook(newEntity)
        }
    }

    override suspend fun updateCategory(book: Book, newCategory: String) {
        val existingBook = bookDao.getBookEntity(book.id)

        if (existingBook != null) {
            bookDao.updateCategory(book.id, newCategory)
        } else {
            val newEntity = book.toEntity(
                category = newCategory,
                status = DownloadStatus.NOT_DOWNLOADED
            )
            bookDao.insertBook(newEntity)
        }
    }

    override suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int) {
        val existingBook = bookDao.getBookEntity(bookId)

        if (existingBook != null) {
            val updatedBook = existingBook.copy(
                progressPercent = progressPercent,
                lastChapterIndex = chapterIndex,
                lastReadTime = System.currentTimeMillis()
            )
            bookDao.insertBook(updatedBook)
        }
    }

    override suspend fun resetReadingProgress(bookId: String) {
        bookDao.resetReadingProgress(bookId)
    }
}