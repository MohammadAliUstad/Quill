package com.yugentech.quill.bookDetails.repository

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
import com.yugentech.quill.aira.rag.BookIndexingWorker
import kotlinx.coroutines.flow.Flow
import java.io.File

//New Repo

class BookDetailsRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val workManager: WorkManager
) : BookDetailsRepository {

    override fun getBook(bookId: String): Flow<BookEntity?> =
        bookDao.getBookEntityFlow(bookId)

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override suspend fun isBookInLibrary(bookId: String): Boolean =
        bookDao.hasBook(bookId)

    override suspend fun getBookOnce(bookId: String): BookEntity? =
        bookDao.getBookEntity(bookId)

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
                )
            )
            .addTag("download_${book.id}")
            .build()

        val indexRequest = OneTimeWorkRequestBuilder<BookIndexingWorker>()
            .setInputData(
                workDataOf(BookIndexingWorker.KEY_BOOK_ID to book.id)
            )
            .addTag("index_${book.id}")
            .build()

        workManager
            .beginWith(downloadRequest)
            .then(indexRequest)
            .enqueue()
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
    }

    override suspend fun updateFavorite(book: Book, isFavorite: Boolean) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateFavorite(book.id, isFavorite)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    isFavorite = isFavorite,
                    status = DownloadStatus.NOT_DOWNLOADED
                )
            )
        }
    }

    override suspend fun updateCategory(book: Book, newCategory: String) {
        if (bookDao.getBookEntity(book.id) != null) {
            bookDao.updateCategory(book.id, newCategory)
        } else {
            bookDao.insertBook(
                book.toEntity(
                    category = newCategory,
                    status = DownloadStatus.NOT_DOWNLOADED
                )
            )
        }
    }

    override suspend fun updateProgress(bookId: String, progressPercent: Float, chapterIndex: Int) {
        bookDao.getBookEntity(bookId)?.let { existing ->
            bookDao.insertBook(
                existing.copy(
                    progressPercent = progressPercent,
                    lastChapterIndex = chapterIndex,
                    lastReadTime = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun resetReadingProgress(bookId: String) {
        bookDao.resetReadingProgress(bookId)
    }
}