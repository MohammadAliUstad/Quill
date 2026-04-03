package com.yugentech.quill.bookDetails.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.bookDetails.EpubParser
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

        // 1. Clean file name matching the Sync logic exactly
        val fileName = "$bookId.epub"

        // 2. Setup internal private directory
        val booksDir = File(applicationContext.filesDir, "books")
        if (!booksDir.exists()) {
            booksDir.mkdirs()
        }

        try {
            // 1. Set Status to Downloading
            bookDao.updateDownloadStatus(bookId, DownloadStatus.DOWNLOADING)

            // 2. Setup the Connection pointing to the hidden folder
            val file = File(booksDir, fileName)
            if (file.exists()) file.delete()

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
            }

            // Get total file size from server headers
            val totalBytes = connection.contentLength.toLong()
            var bytesDownloaded = 0L

            // 3. Download with Buffered Progress Tracking
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8 * 1024) // 8KB buffer
                    var bytesRead: Int
                    var lastProgressUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val currentTime = System.currentTimeMillis()
                        // Throttle progress updates to every 500ms to keep the UI smooth
                        if (currentTime - lastProgressUpdate > 500) {
                            val progressPercent = if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100f
                            } else {
                                0f
                            }

                            // Broadcast progress to the ViewModel
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

            // Force 100% broadcast when finished
            setProgress(
                workDataOf(
                    "PROGRESS_PERCENT" to 100f,
                    "BYTES_DOWNLOADED" to bytesDownloaded,
                    "TOTAL_BYTES" to totalBytes
                )
            )

            // 4. Parse the Epub
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

                // --- NEW: TRIGGER THE INDEXING QUEUE ON SUCCESS ---
                val isProUser = inputData.getBoolean("IS_PRO_USER", false)
                if (isProUser) {
                    val indexRequest = androidx.work.OneTimeWorkRequestBuilder<com.yugentech.quill.aira.rag.BookEmbeddingWorker>()
                        .setInputData(androidx.work.workDataOf(com.yugentech.quill.aira.rag.BookEmbeddingWorker.KEY_BOOK_ID to bookId))
                        .addTag("AI_INDEXING")
                        .addTag("index_${bookId}")
                        .build()

                    androidx.work.WorkManager.getInstance(applicationContext).beginUniqueWork(
                        "BOOK_PROCESSING_QUEUE",
                        androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, // Funnel into the 1-by-1 queue!
                        indexRequest
                    ).enqueue()
                }

                Result.success()
            } else {
                Timber.Forest.e("Book not found in DB: $bookId")
                Result.failure()
            }

        } catch (e: Exception) {
            Timber.Forest.e(e, "Download failed")

            // Ensure we delete from the correct internal folder on failure
            val file = File(booksDir, fileName)
            if (file.exists()) file.delete()

            bookDao.updateDownloadStatus(bookId, DownloadStatus.FAILED)
            Result.failure()
        }
    }
}