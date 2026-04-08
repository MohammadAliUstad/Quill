package com.yugentech.quill.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.bookDetails.EpubParser
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object LocalBookImporter {

    suspend fun importFiles(
        context: Context,
        bookDao: BookDao,
        uris: List<Uri>,
        isPro: Boolean
    ): List<ImportResult> = withContext(Dispatchers.IO) {
        uris.map { uri -> importSingle(context, bookDao, uri, isPro) }
    }

    private suspend fun importSingle(
        context: Context,
        bookDao: BookDao,
        uri: Uri,
        isPro: Boolean
    ): ImportResult {
        val fileName = resolveFileName(context, uri) ?: "unknown.epub"
        val bookId = "local_${uri.hashCode()}"

        return try {
            val importsDir = File(context.filesDir, "imports").also { it.mkdirs() }
            val destFile = File(importsDir, "$bookId.epub")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return ImportResult.Failure(fileName, "Could not open file")

            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
            val opener = PublicationOpener(parser, emptyList(), onCreatePublication = {})

            val asset = assetRetriever.retrieve(destFile)
                .getOrElse { return ImportResult.Failure(fileName, "Not a valid EPUB") }

            val publication = opener.open(asset, allowUserInteraction = false)
                .getOrElse { return ImportResult.Failure(fileName, "Could not open EPUB") }

            val metadata = publication.metadata

            val title = metadata.title?.trim()
                ?.ifBlank { null }
                ?: fileName.removeSuffix(".epub")

            val author = metadata.authors
                .joinToString(" & ") { it.name.trim() }
                .ifBlank { "Unknown Author" }

            val description = metadata.description?.trim()?.ifBlank { null }
            val language = metadata.languages.firstOrNull() ?: "en"
            val subjects = metadata.subjects.map { it.name.trim() }.filter { it.isNotBlank() }

            val coverUrl = saveCover(context, publication.cover(), bookId)

            publication.close()

            val entity = BookEntity(
                id = bookId,
                title = title,
                author = author,
                description = description,
                coverUrl = coverUrl,
                downloadUrl = "",
                source = BookSource.USER_IMPORTED,
                subjects = subjects,
                language = language,
                localFilePath = destFile.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADED,
                addedAt = System.currentTimeMillis()
            )

            bookDao.insertBook(entity)

            val epubParser = EpubParser(context)
            val parsed = epubParser.parse(destFile.absolutePath, title)

            val updatedEntity = entity.copy(
                chapters = parsed.chapters,
                totalPages = parsed.totalPages,
                fileSizeBytes = destFile.length()
            )
            bookDao.insertBook(updatedEntity)

            if (isPro) {
                val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
                    .setInputData(
                        workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to bookId)
                    )
                    .addTag("index_$bookId")
                    .addTag("AI_INDEXING")
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "global_book_processing_queue",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    indexRequest
                )
            }

            ImportResult.Success(bookId, title)

        } catch (e: Exception) {
            Timber.e(e, "Import failed for $fileName")
            ImportResult.Failure(fileName, e.localizedMessage ?: "Unknown error")
        }
    }

    private fun saveCover(context: Context, bitmap: Bitmap?, bookId: String): String? {
        bitmap ?: return null
        return try {
            val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
            val coverFile = File(coversDir, "$bookId.jpg")
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            Timber.w(e, "Failed to save cover for $bookId")
            null
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (_: Exception) {
            null
        }
    }
}