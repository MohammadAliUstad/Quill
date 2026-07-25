package com.yugentech.quill.bookDetails.repository

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.bookDetails.worker.BookDownloadWorker
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.mapper.toEntity
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookDetailsRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val chunkDao: BookChunkDao,
    private val indexingStateDao: BookIndexingStateDao,
    private val workManager: WorkManager,
    private val cloudSyncRepository: CloudSyncRepository
) : BookDetailsRepository {

    override fun getBook(bookId: String): Flow<BookEntity?> =
        bookDao.getBookEntityFlow(bookId)

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override suspend fun isBookInLibrary(bookId: String): Boolean =
        bookDao.hasBook(bookId)

    override suspend fun getBookOnce(bookId: String): BookEntity? =
        bookDao.getBookEntity(bookId)

    override suspend fun startDownload(book: Book, isPro: Boolean) {
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
            userCategory = existingBook?.userCategory ?: SHELF,
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
                    "BOOK_TITLE" to book.title,
                    "IS_PRO_USER" to isPro
                )
            )
            .addTag("download_${book.id}")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                java.util.concurrent.TimeUnit.SECONDS.toMillis(30),
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_${book.id}",
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun removeDownload(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
        bookDao.removeDownload(bookId)
    }

    override suspend fun deleteBook(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
        bookDao.deleteBook(bookId)
        cloudSyncRepository.deleteBookFromCloud(bookId)
    }

    override suspend fun updateFavorite(book: Book, isFavorite: Boolean) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateFavorite(book.id, isFavorite)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    isFavorite = isFavorite,
                    status = DownloadStatus.NOT_DOWNLOADED
                ).copy(isSynced = false)
            )
        }
        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun updateCategory(book: Book, newCategory: String) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateCategory(book.id, newCategory)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    category = newCategory,
                    status = DownloadStatus.NOT_DOWNLOADED
                ).copy(isSynced = false)
            )
        }
        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int) {
        val existing = bookDao.getBookEntity(bookId)
        if (existing != null) {
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

    override suspend fun resetReadingProgress(bookId: String) {
        bookDao.resetReadingProgress(bookId)
        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun removeFromRecent(bookId: String) {
        bookDao.removeFromRecent(bookId)
    }

    override fun observeIsReady(bookId: String): Flow<Boolean> {
        return indexingStateDao.observeIsComplete(bookId)
    }

    override suspend fun isReady(bookId: String): Boolean {
        return chunkDao.isBookIndexed(bookId)
    }

    override suspend fun isSpoilerLockEnabled(bookId: String): Boolean {
        return bookDao.getBookEntity(bookId)?.spoilerLockEnabled ?: true
    }

    override suspend fun setSpoilerLock(bookId: String, enabled: Boolean) {
        bookDao.updateSpoilerLock(bookId, enabled)
    }

    override suspend fun getUnindexedDownloadedBooks(): List<BookEntity> {
        return bookDao.getUnindexedDownloadedBooks()
    }

    override suspend fun enqueueIndexing(bookId: String) {
        val request = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
            .setInputData(workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to bookId))
            .addTag("index_$bookId")
            .addTag("AI_INDEXING")
            .build()

        workManager.enqueueUniqueWork(
            "global_book_processing_queue",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}