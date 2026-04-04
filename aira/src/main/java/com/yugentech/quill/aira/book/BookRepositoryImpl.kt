package com.yugentech.quill.aira.book

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val indexingStateDao: BookIndexingStateDao,
    private val workManager: WorkManager
) : BookRepository {

    override suspend fun getBookDetails(bookId: String): BookEntity? {
        return bookDao.getBookEntity(bookId)
    }

    // In BookRepository.kt
    override suspend fun observeIsReady(bookId: String): Flow<Boolean> {
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

    override suspend fun indexLibraryBacklog() {
        val unindexedBooks = bookDao.getUnindexedDownloadedBooks()

        unindexedBooks.forEach { book ->
            val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
                .setInputData(workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to book.id))
                .addTag("AI_INDEXING")
                .addTag("index_${book.id}")
                .build()

            workManager.beginUniqueWork(
                "BOOK_PROCESSING_QUEUE",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                indexRequest
            ).enqueue()
        }
    }
}