package com.yugentech.quill.ui.tabs.sourcesScreen.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.utils.EpubParser
import com.yugentech.theme.tokens.AppConstants
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
import com.yugentech.quill.R
import com.yugentech.quill.localization.AppStrings

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

        // The same file picked again: stop before copying anything, so the book's file and
        // reading progress stay exactly as they are
        bookDao.getBookEntity(bookId)?.takeIf { it.hasFile() }?.let {
            return ImportResult.AlreadyExists(it.title)
        }

        val importsDir = File(context.filesDir, "imports").also { it.mkdirs() }
        // Copied under a temporary name until it's known not to be a book already in the library
        val tempFile = File(importsDir, "$bookId.importing")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return ImportResult.Failure(fileName, AppStrings.get(R.string.import_error_open_file))

            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
            val opener = PublicationOpener(parser, emptyList(), onCreatePublication = {})

            val asset = assetRetriever.retrieve(tempFile)
                .getOrElse { return ImportResult.Failure(fileName, AppStrings.get(R.string.import_error_not_epub)) }

            val publication = opener.open(asset, allowUserInteraction = false)
                .getOrElse { return ImportResult.Failure(fileName, AppStrings.get(R.string.import_error_open_epub)) }

            val metadata = publication.metadata

            val title = metadata.title?.trim()
                ?.ifBlank { null }
                ?: fileName.removeSuffix(".epub")

            val author = metadata.authors
                .joinToString(" & ") { it.name.trim() }
                .ifBlank { AppStrings.get(R.string.unknown_author) }

            // Already in the library: this file's own record, or the same book imported from
            // another file. Blocked, unless that book has no file on this device (it came from
            // another device through sync); then this file is attached and its progress kept.
            val existing = bookDao.getBookEntity(bookId) ?: bookDao.findImportedBook(title, author)
            if (existing?.hasFile() == true) {
                publication.close()
                return ImportResult.AlreadyExists(existing.title)
            }

            val id = existing?.id ?: bookId
            val description = metadata.description?.trim()?.ifBlank { null }
            val language = metadata.languages.firstOrNull() ?: "en"
            val subjects = metadata.subjects.map { it.name.trim() }.filter { it.isNotBlank() }
            val cover = publication.cover()

            publication.close()

            val destFile = File(importsDir, "$id.epub")
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
            }
            val coverUrl = saveCover(context, cover, id)

            val entity = existing?.copy(
                coverUrl = coverUrl ?: existing.coverUrl,
                localFilePath = destFile.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADED,
                downloadError = null
            ) ?: BookEntity(
                id = id,
                title = title,
                author = author,
                description = description,
                coverUrl = coverUrl,
                downloadUrl = AppConstants.EMPTY,
                source = BookSource.USER_IMPORTED,
                subjects = subjects,
                language = language,
                localFilePath = destFile.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADED,
                addedAt = System.currentTimeMillis()
            )

            // The chapter list and page count; a book Readium opened is still added without them
            val parsed = runCatching { EpubParser(context).parse(destFile.absolutePath, title) }
                .onFailure { Timber.w(it, "Couldn't parse chapters of $fileName") }
                .getOrNull()

            bookDao.insertBook(
                if (parsed == null) {
                    entity.copy(fileSizeBytes = destFile.length())
                } else {
                    entity.copy(
                        chapters = parsed.chapters,
                        totalPages = parsed.totalPages,
                        fileSizeBytes = destFile.length()
                    )
                }
            )

            ImportResult.Success(id, entity.title)

        } catch (e: Exception) {
            Timber.e(e, "Import failed for $fileName")
            ImportResult.Failure(fileName, e.localizedMessage ?: AppStrings.get(R.string.unknown_error))
        } finally {
            // Left behind only when the import stopped before the file was moved into place
            tempFile.delete()
        }
    }

    /** Whether this book's file is on this device. */
    private fun BookEntity.hasFile(): Boolean =
        downloadStatus == DownloadStatus.DOWNLOADED &&
            localFilePath?.let { File(it).exists() } == true

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