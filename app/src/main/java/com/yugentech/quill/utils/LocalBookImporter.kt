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
        uris: List<Uri>
    ): List<ImportResult> = withContext(Dispatchers.IO) {
        uris.map { uri -> importSingle(context, bookDao, uri) }
    }

    private suspend fun importSingle(
        context: Context,
        bookDao: BookDao,
        uri: Uri
    ): ImportResult {
        val fileName = resolveFileName(context, uri) ?: "unknown.epub"
        val bookId = "local_${uri.hashCode()}"

        return try {
            // ── Step 1: Copy to internal storage ─────────────────────────
            val importsDir = File(context.filesDir, "imports").also { it.mkdirs() }
            val destFile = File(importsDir, "$bookId.epub")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return ImportResult.Failure(fileName, "Could not open file")

            // ── Step 2: Open with Readium and extract metadata ────────────
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

            // ── Step 3: Save cover image ──────────────────────────────────
            val coverUrl = saveCover(context, publication.cover(), bookId)

            publication.close()

            // ── Step 4: Insert into Room as DOWNLOADED ────────────────────
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

            // ── Step 5: Parse chapters in background and update ───────────
            val epubParser = EpubParser(context)
            val parsed = epubParser.parse(destFile.absolutePath, title)

            val updatedEntity = entity.copy(
                chapters = parsed.chapters,
                totalPages = parsed.totalPages,
                fileSizeBytes = destFile.length()
            )
            bookDao.insertBook(updatedEntity)

            // ── Step 6: Trigger Aira Indexing (Matched to Repo Setup) ─────
            val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
                .setInputData(
                    workDataOf(BookEmbeddingWorker.KEY_BOOK_ID to bookId)
                )
                .addTag("index_$bookId")
                .addTag("AI_INDEXING") // Added missing tag to match repo
                .build()

            // Use beginUniqueWork with REPLACE policy exactly like the repository
            WorkManager.getInstance(context)
                .beginUniqueWork(
                    "global_book_processing_queue", // <-- Match the exact same global name
                    ExistingWorkPolicy.APPEND_OR_REPLACE, // <-- Queue them up
                    indexRequest
                )
                .enqueue()

            ImportResult.Success(bookId, title)

        } catch (e: Exception) {
            Timber.e(e, "Import failed for $fileName")
            ImportResult.Failure(fileName, e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Saves the cover [Bitmap] to internal storage and returns its file:// path,
     * or null if no cover is available.
     */
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