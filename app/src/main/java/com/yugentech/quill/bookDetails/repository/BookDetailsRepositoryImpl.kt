package com.yugentech.quill.bookDetails.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.mapper.toEntity
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.bookDetails.worker.BookDownloadWorker
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.cloud.repository.CloudSyncRepository // <-- NEW IMPORT
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookDetailsRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val workManager: WorkManager,
    private val cloudSyncRepository: CloudSyncRepository // <-- INJECTED SCHEDULER
) : BookDetailsRepository {

    override fun getBook(bookId: String): Flow<BookEntity?> =
        bookDao.getBookEntityFlow(bookId)

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override suspend fun isBookInLibrary(bookId: String): Boolean =
        bookDao.hasBook(bookId)

    override suspend fun getBookOnce(bookId: String): BookEntity? =
        bookDao.getBookEntity(bookId)

    // LOCAL ONLY: Downloading a file doesn't affect cloud sync status
    override suspend fun startDownload(book: Book) {
        val existingBook = bookDao.getBookEntity(book.id)

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
            isFavorite = existingBook?.isFavorite ?: false,
            isSynced = false,
            userCategory = existingBook?.userCategory ?: "Shelf",
            addedAt = existingBook?.addedAt ?: System.currentTimeMillis(),
            progressPercent = existingBook?.progressPercent ?: 0f,
            totalPages = existingBook?.totalPages ?: 0,
            lastChapterTitle = existingBook?.lastChapterTitle,
            lastReadTime = existingBook?.lastReadTime ?: 0,
            lastChapterIndex = existingBook?.lastChapterIndex ?: 0,
            lastScrollPosition = existingBook?.lastScrollPosition ?: 0,
            lastLocatorJson = existingBook?.lastLocatorJson
        )

        bookDao.insertBook(newEntity)

        val downloadRequest = OneTimeWorkRequestBuilder<BookDownloadWorker>()
            .setInputData(
                workDataOf(
                    "BOOK_ID" to book.id,
                    "DOWNLOAD_URL" to book.downloadUrl,
                    "BOOK_TITLE" to book.title
                    // "IS_PRO_USER" to isProUser // <-- Add this back here if you are passing it into the function
                )
            )
            .addTag("download_${book.id}")
            .build()

        val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
            .setInputData(
                workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to book.id)
            )
            .addTag("index_${book.id}")
            .addTag("AI_INDEXING")
            .build()

        // Use beginUniqueWork to apply the REPLACE policy to the whole chain
        workManager
            .beginUniqueWork(
                "global_book_processing_queue", // <-- Use ONE global name
                ExistingWorkPolicy.APPEND_OR_REPLACE, // <-- Queue them up
                downloadRequest
            )
            .then(indexRequest)
            .enqueue()

        cloudSyncRepository.scheduleBackgroundSync()
    }

    // LOCAL ONLY: Removing a file doesn't erase cloud reading progress
    override suspend fun removeDownload(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
        bookDao.removeDownload(bookId)
    }

    // CLOUD SYNC: Full deletion happens instantly (no timer)
    override suspend fun deleteBook(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }

        // Delete locally
        bookDao.deleteBook(bookId)
        // Delete from cloud immediately
        cloudSyncRepository.deleteBookFromCloud(bookId)
    }

    // CLOUD SYNC: Trigger 15s timer
    override suspend fun updateFavorite(book: Book, isFavorite: Boolean) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateFavorite(book.id, isFavorite)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    isFavorite = isFavorite,
                    status = DownloadStatus.NOT_DOWNLOADED
                ).copy(isSynced = false) // Ensure new inserts are flagged
            )
        }
        cloudSyncRepository.scheduleBackgroundSync()
    }

    // CLOUD SYNC: Trigger 15s timer
    override suspend fun updateCategory(book: Book, newCategory: String) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateCategory(book.id, newCategory)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    category = newCategory,
                    status = DownloadStatus.NOT_DOWNLOADED
                ).copy(isSynced = false) // Ensure new inserts are flagged
            )
        }
        cloudSyncRepository.scheduleBackgroundSync()
    }

    // CLOUD SYNC: Trigger 15s timer + Bug Fixed
    override suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int) {
        val existing = bookDao.getBookEntity(bookId)
        if (existing != null) {
            // BUG FIX: Use the targeted DAO method so 'isSynced = 0' gets applied
            // and we don't accidentally wipe out lastChapterTitle or lastLocatorJson
            bookDao.updateReadingProgress(
                bookId = bookId,
                progress = progressPercent,
                chapterTitle = existing.lastChapterTitle,
                chapterIndex = chapterIndex,
                locatorJson = existing.lastLocatorJson,
                readTime = System.currentTimeMillis()
            )
            cloudSyncRepository.scheduleBackgroundSync()
        }
    }

    // CLOUD SYNC: Trigger 15s timer
    override suspend fun resetReadingProgress(bookId: String) {
        bookDao.resetReadingProgress(bookId)
        cloudSyncRepository.scheduleBackgroundSync()
    }
}