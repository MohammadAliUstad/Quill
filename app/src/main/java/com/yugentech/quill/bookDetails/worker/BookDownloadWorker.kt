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
import java.net.HttpURLConnection
import java.net.URL

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
        if (!booksDir.exists()) {
            booksDir.mkdirs()
        }

        try {
            bookDao.updateDownloadStatus(bookId, DownloadStatus.DOWNLOADING)

            val file = File(booksDir, fileName)
            if (file.exists()) file.delete()

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
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

            setProgress(
                workDataOf(
                    "PROGRESS_PERCENT" to 100f,
                    "BYTES_DOWNLOADED" to bytesDownloaded,
                    "TOTAL_BYTES" to totalBytes
                )
            )

            val parser = EpubParser(applicationContext)
            val parsedData = parser.parse(file.absolutePath, bookTitle)

            val existingBook = bookDao.getBookEntity(bookId)

            if (existingBook != null) {
                val updatedBook = existingBook.copy(
                    localFilePath = file.absolutePath,
                    downloadStatus = DownloadStatus.DOWNLOADED,
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
                Timber.e("Book not found in DB: $bookId")
                Result.failure()
            }

        } catch (e: Exception) {
            Timber.e(e, "Download failed")

            val file = File(booksDir, fileName)
            if (file.exists()) file.delete()

            bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED)
            Result.failure()
        }
    }
}