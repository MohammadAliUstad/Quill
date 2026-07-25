package com.yugentech.quill.bookDetails.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager.Companion.getInstance
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.utils.EpubParser
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.net.ConnectException

class BookDownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val bookDao: BookDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookId = inputData.getString("BOOK_ID") ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString("DOWNLOAD_URL") ?: return@withContext Result.failure()
        val bookTitle = inputData.getString("BOOK_TITLE") ?: "Unknown"

        val fileName = "$bookId.epub"
        val booksDir = File(applicationContext.filesDir, "books")
        if (!booksDir.exists()) booksDir.mkdirs()
        val file = File(booksDir, fileName)

        try {
            bookDao.updateDownloadStatus(bookId, DownloadStatus.DOWNLOADING, null)

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = when (connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> "Book file not found on server."
                    HttpURLConnection.HTTP_FORBIDDEN -> "Access to book file denied."
                    HttpURLConnection.HTTP_UNAVAILABLE -> "Server is currently unavailable. Please try again later."
                    else -> "Server error (HTTP ${connection.responseCode})"
                }
                bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED, errorMsg)
                return@withContext Result.failure()
            }

            val totalBytes = connection.contentLength.toLong()
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var lastProgressUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressUpdate > 500) {
                            val progressPercent = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100f
                            } else {
                                0f
                            }

                            setProgress(
                                workDataOf(
                                    "PROGRESS_PERCENT" to progressPercent,
                                    "BYTES_DOWNLOADED" to bytesDownloaded,
                                    "TOTAL_BYTES" to totalBytes
                                )
                            )
                            lastProgressUpdate = currentTime
                        }
                    }
                }
            }

            val parser = EpubParser(applicationContext)
            val parsedData = parser.parse(file.absolutePath, bookTitle)

            val existingBook = bookDao.getBookEntity(bookId)
            if (existingBook != null) {
                val updatedBook = existingBook.copy(
                    localFilePath = file.absolutePath,
                    downloadStatus = DownloadStatus.DOWNLOADED,
                    downloadError = null,
                    fileSizeBytes = file.length(),
                    chapters = parsedData.chapters,
                    totalPages = parsedData.totalPages
                )

                bookDao.insertBook(updatedBook)

                val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
                    .setInputData(workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to bookId))
                    .addTag("AI_INDEXING")
                    .addTag("index_${bookId}")
                    .build()

                getInstance(applicationContext).beginUniqueWork(
                    "BOOK_PROCESSING_QUEUE",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    indexRequest
                ).enqueue()

                Result.success()
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Timber.e(e, "Download failed for $bookId")
            
            if (file.exists()) file.delete()

            return@withContext when (e) {
                is UnknownHostException, is ConnectException -> {
                    // Network issue, let WorkManager retry
                    bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED, "No internet connection. Waiting for network...")
                    Result.retry()
                }
                is IOException -> {
                    if (e.message?.contains("ENOSPC", ignoreCase = true) == true) {
                        bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED, "Insufficient storage space on device.")
                    } else {
                        bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED, "A network error occurred. Please try again.")
                    }
                    Result.failure()
                }
                else -> {
                    bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED, "An unexpected error occurred during download.")
                    Result.failure()
                }
            }
        }
    }
}
