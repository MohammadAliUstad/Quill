package com.yugentech.quill.auth.guest

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.dao.HighlightDao
import com.yugentech.quill.database.dao.ReadingSessionDao
import com.yugentech.quill.database.dao.UserDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.utils.EpubParser
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Saves a guest's library to one JSON file they choose, and reads such a file back. A guest has
 * no account to sign in to again, so this is how they carry their reading to another phone or
 * through a reinstall. A signed-in user can import one too, which is how a guest's library comes
 * into a new account (its books and categories then sync to Firestore like the rest).
 *
 * The file holds the profile's name and avatar, the guest's own categories, the books they added
 * from a source (with their place, progress, shelf and favourite), and those books' highlights
 * and reading sessions. Books imported from files aren't in it: a backup can't carry the book
 * files themselves. Books from a source download again from that source after an import.
 *
 * Importing merges, never replaces: books and highlights already here (by ID), reading sessions
 * already here (same book and start time) and categories with a name already here are skipped,
 * so importing the same file twice changes nothing. The file may have been edited or damaged, so
 * every entry is checked and bad ones are dropped rather than failing the whole file.
 */
class GuestBackupManager(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val highlightDao: HighlightDao,
    private val readingSessionDao: ReadingSessionDao,
    private val userDao: UserDao
) {

    /** What an import added. */
    data class ImportSummary(val booksAdded: Int, val highlightsAdded: Int)

    /** Writes the guest's data to [uri]. Returns false when the file couldn't be written. */
    suspend fun export(userId: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put(KEY_APP, APP_ID)
                .put(KEY_VERSION, FORMAT_VERSION)
                .put(KEY_EXPORTED_AT, System.currentTimeMillis())

            userDao.getUser(userId)?.let { user ->
                json.put(KEY_PROFILE, JSONObject().apply {
                    user.name?.let { put("name", it) }
                    user.avatarId?.let { put("avatarId", it) }
                })
            }

            json.put(KEY_CATEGORIES, JSONArray().apply {
                categoryDao.getAllCategoriesOnce().filterNot { it.isSystem }.forEach { c ->
                    put(JSONObject().put("name", c.name).put("sortOrder", c.sortOrder))
                }
            })

            // A book imported from a file has nothing to download it again from, so it (and what
            // hangs off it) stays out of the file
            val books = bookDao.getAllBooksOnce().filter { it.source != BookSource.USER_IMPORTED }
            val bookIds = books.mapTo(HashSet()) { it.id }

            json.put(KEY_BOOKS, JSONArray().apply {
                books.forEach { b ->
                    put(JSONObject()
                        .put("id", b.id)
                        .put("title", b.title)
                        .put("author", b.author)
                        .put("source", b.source.name)
                        .put("downloadUrl", b.downloadUrl)
                        .put("userCategory", b.userCategory)
                        .put("isFavorite", b.isFavorite)
                        .put("addedAt", b.addedAt)
                        .put("progressPercent", b.progressPercent.toDouble())
                        .put("lastChapterIndex", b.lastChapterIndex)
                        .put("lastReadTime", b.lastReadTime)
                        .put("spoilerLockEnabled", b.spoilerLockEnabled)
                        .put("subjects", JSONArray(b.subjects))
                        .apply {
                            b.coverUrl?.let { put("coverUrl", it) }
                            b.description?.let { put("description", it) }
                            b.language?.let { put("language", it) }
                            b.lastChapterTitle?.let { put("lastChapterTitle", it) }
                            b.lastLocatorJson?.let { put("lastLocatorJson", it) }
                        })
                }
            })

            json.put(KEY_HIGHLIGHTS, JSONArray().apply {
                highlightDao.getAllHighlightsOnce().filter { it.bookId in bookIds }.forEach { h ->
                    put(JSONObject()
                        .put("id", h.id)
                        .put("bookId", h.bookId)
                        .put("locatorJson", h.locatorJson)
                        .put("colorInt", h.colorInt)
                        .put("createdAt", h.createdAt))
                }
            })

            json.put(KEY_READING_SESSIONS, JSONArray().apply {
                readingSessionDao.getAllSessionsOnce().filter { it.bookId in bookIds }.forEach { s ->
                    put(JSONObject()
                        .put("bookId", s.bookId)
                        .put("startTime", s.startTime)
                        .put("endTime", s.endTime)
                        .put("durationMillis", s.durationMillis))
                }
            })

            // "wt" truncates an existing file; a few providers only know "w"
            val stream = runCatching { contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: contentResolver.openOutputStream(uri, "w")
                ?: return@withContext false
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(json.toString(2)) }
            true
        } catch (e: Exception) {
            Timber.e(e, "Guest backup export failed")
            false
        }
    }

    /**
     * Adds what's in the backup at [uri] to this phone's library. Returns null when the file isn't
     * a Quill backup (or is too damaged to read at all). For a guest what's added stays on this
     * phone; for an account it's marked unsynced, so the next sync uploads it.
     */
    suspend fun import(userId: String, uri: Uri): ImportSummary? = withContext(Dispatchers.IO) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readAtMost(MAX_FILE_BYTES)
                bytes?.toString(Charsets.UTF_8)
            } ?: return@withContext null

            val json = JSONObject(text)
            if (json.optString(KEY_APP) != APP_ID) return@withContext null
            val version = json.optInt(KEY_VERSION, -1)
            if (version !in 1..FORMAT_VERSION) return@withContext null

            // Categories first, so the books' shelves exist when the books arrive
            importCategories(json.optJSONArray(KEY_CATEGORIES))
            val booksAdded = importBooks(json.optJSONArray(KEY_BOOKS))
            val highlightsAdded = importHighlights(json.optJSONArray(KEY_HIGHLIGHTS))
            importReadingSessions(json.optJSONArray(KEY_READING_SESSIONS))
            json.optJSONObject(KEY_PROFILE)?.let { importProfile(userId, it) }

            ImportSummary(booksAdded, highlightsAdded)
        } catch (e: Exception) {
            Timber.e(e, "Guest backup import failed")
            null
        }
    }

    private suspend fun importCategories(array: JSONArray?) {
        if (array == null) return
        val existing = categoryDao.getAllCategoriesOnce()
        val names = existing.mapTo(HashSet()) { it.name }
        var nextOrder = existing.filterNot { it.isSystem }.size
        val added = mutableListOf<CategoryEntity>()
        for (i in 0 until minOf(array.length(), MAX_CATEGORIES)) {
            val o = array.optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            if (name.isEmpty() || name.length > MAX_TEXT || name in names) continue
            added += CategoryEntity(
                name = name,
                sortOrder = nextOrder++,
                isSystem = false,
                isSynced = false
            )
            names += name
        }
        added.forEach { categoryDao.insertCategory(it) }
    }

    private suspend fun importBooks(array: JSONArray?): Int {
        if (array == null) return 0
        val now = System.currentTimeMillis()
        val shelves = categoryDao.getAllCategoriesOnce().mapTo(HashSet()) { it.name } + SHELF
        val parsed = (0 until minOf(array.length(), MAX_BOOKS)).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").trim()
            val title = o.optString("title").trim().take(MAX_TEXT)
            val source = runCatching { BookSource.valueOf(o.optString("source")) }.getOrNull()
            val downloadUrl = o.optString("downloadUrl").trim()
            val progress = o.optDouble("progressPercent", 0.0).toFloat()
            when {
                id.isEmpty() || id.length > MAX_TEXT || title.isEmpty() -> null
                source == null || source == BookSource.USER_IMPORTED -> null
                downloadUrl.isEmpty() || downloadUrl.length > MAX_LONG_TEXT -> null
                progress.isNaN() || progress !in 0f..1f -> null
                else -> o.toBookEntity(id, title, source, downloadUrl, progress, shelves, now)
            }
        }.distinctBy { it.id }

        // Skip books already here: this phone's copy has the newer progress
        val existing = parsed.map { it.id }.chunked(QUERY_CHUNK)
            .flatMapTo(HashSet()) { bookDao.getExistingIds(it) }
        val added = parsed.filter { it.id !in existing }.map { it.withLocalFile() }
        if (added.isNotEmpty()) bookDao.insertBooks(added)
        return added.size
    }

    private fun JSONObject.toBookEntity(
        id: String,
        title: String,
        source: BookSource,
        downloadUrl: String,
        progress: Float,
        shelves: Set<String>,
        now: Long
    ): BookEntity {
        val subjects = optJSONArray("subjects")?.let { a ->
            (0 until minOf(a.length(), MAX_SUBJECTS)).mapNotNull { j ->
                a.optString(j).trim().takeIf { it.isNotEmpty() && it.length <= MAX_TEXT }
            }
        }.orEmpty()
        return BookEntity(
            id = id,
            title = title,
            author = optString("author").trim().take(MAX_TEXT),
            coverUrl = optText("coverUrl", MAX_LONG_TEXT),
            source = source,
            downloadUrl = downloadUrl,
            description = optText("description", MAX_LONG_TEXT),
            subjects = subjects,
            language = optText("language", MAX_TEXT),
            // A shelf the file names but doesn't bring along falls back to the main shelf
            userCategory = optString("userCategory").trim().takeIf { it in shelves } ?: SHELF,
            isFavorite = optBoolean("isFavorite", false),
            addedAt = optLong("addedAt", now).takeIf { it in 1..(now + DAY_MS) } ?: now,
            progressPercent = progress,
            lastChapterTitle = optText("lastChapterTitle", MAX_TEXT),
            lastChapterIndex = optInt("lastChapterIndex", 0).coerceAtLeast(0),
            lastLocatorJson = optText("lastLocatorJson", MAX_LONG_TEXT),
            lastReadTime = optLong("lastReadTime", 0L).takeIf { it in 0..(now + DAY_MS) } ?: 0L,
            spoilerLockEnabled = optBoolean("spoilerLockEnabled", true),
            // Unsynced, so a signed-in import uploads it (a guest's never leaves the phone)
            isSynced = false
        )
    }

    /** Picks up the book's file if it's already on this phone, as the account sync does. */
    private suspend fun BookEntity.withLocalFile(): BookEntity {
        val file = File(context.filesDir, "books/$id.epub")
        if (!file.exists()) return this
        var chapters: List<Chapter> = emptyList()
        var totalPages = 0
        try {
            val parsed = EpubParser(context).parse(file.absolutePath, title)
            chapters = parsed.chapters
            totalPages = parsed.totalPages
        } catch (e: Exception) {
            Timber.e(e, "Couldn't read the file already on this phone for an imported book")
        }
        return copy(
            downloadStatus = DownloadStatus.DOWNLOADED,
            localFilePath = file.absolutePath,
            fileSizeBytes = file.length(),
            chapters = chapters,
            totalPages = totalPages
        )
    }

    private suspend fun importHighlights(array: JSONArray?): Int {
        if (array == null) return 0
        val parsed = (0 until minOf(array.length(), MAX_HIGHLIGHTS)).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").trim()
            val bookId = o.optString("bookId").trim()
            val locator = o.optString("locatorJson")
            when {
                id.isEmpty() || id.length > MAX_TEXT || bookId.isEmpty() -> null
                locator.isBlank() || locator.length > MAX_LONG_TEXT -> null
                !o.has("colorInt") -> null
                else -> HighlightEntity(
                    id = id,
                    bookId = bookId,
                    locatorJson = locator,
                    colorInt = o.optInt("colorInt"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.distinctBy { it.id }

        // Only for books on this phone (a highlight can't outlive its book), and not twice
        val books = parsed.map { it.bookId }.distinct().chunked(QUERY_CHUNK)
            .flatMapTo(HashSet()) { bookDao.getExistingIds(it) }
        val existing = parsed.map { it.id }.chunked(QUERY_CHUNK)
            .flatMapTo(HashSet()) { highlightDao.getExistingIds(it) }
        val added = parsed.filter { it.bookId in books && it.id !in existing }
        if (added.isNotEmpty()) highlightDao.insertHighlights(added)
        return added.size
    }

    private suspend fun importReadingSessions(array: JSONArray?) {
        if (array == null) return
        val now = System.currentTimeMillis()
        val parsed = (0 until minOf(array.length(), MAX_SESSIONS)).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val bookId = o.optString("bookId").trim()
            val start = o.optLong("startTime", -1)
            val end = o.optLong("endTime", -1)
            val duration = o.optLong("durationMillis", -1)
            when {
                bookId.isEmpty() || bookId.length > MAX_TEXT -> null
                start <= 0 || end < start || end > now + DAY_MS -> null
                duration !in 1..MAX_SESSION_MS -> null
                else -> ReadingSessionEntity(
                    bookId = bookId,
                    startTime = start,
                    endTime = end,
                    durationMillis = duration
                )
            }
        }.distinctBy { it.bookId to it.startTime }

        val books = parsed.map { it.bookId }.distinct().chunked(QUERY_CHUNK)
            .flatMapTo(HashSet()) { bookDao.getExistingIds(it) }
        val existing = readingSessionDao.getAllSessionsOnce().mapTo(HashSet()) { it.bookId to it.startTime }
        val added = parsed.filter { it.bookId in books && (it.bookId to it.startTime) !in existing }
        if (added.isNotEmpty()) readingSessionDao.insertSessions(added)
    }

    /** Takes the backup's name and avatar only if the user hasn't named themselves here yet. */
    private suspend fun importProfile(userId: String, profile: JSONObject) {
        val user = userDao.getUser(userId) ?: return
        if (!user.name.isNullOrBlank()) return
        val name = profile.optString("name").trim().take(MAX_TEXT).ifEmpty { null } ?: return
        val avatarId = if (profile.has("avatarId")) profile.optInt("avatarId") else user.avatarId
        userDao.saveUser(user.copy(name = name, avatarId = avatarId))
    }

    /** A string field, trimmed, or null when it's missing, blank or longer than [max]. */
    private fun JSONObject.optText(key: String, max: Int): String? =
        if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() && it.length <= max }

    /** Reads at most [limit] bytes; null if the stream holds more, so a huge file isn't loaded. */
    private fun java.io.InputStream.readAtMost(limit: Int): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (out.size() + read > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val APP_ID = "com.yugentech.quill"
        const val FORMAT_VERSION = 1

        const val KEY_APP = "app"
        const val KEY_VERSION = "version"
        const val KEY_EXPORTED_AT = "exportedAt"
        const val KEY_PROFILE = "profile"
        const val KEY_CATEGORIES = "categories"
        const val KEY_BOOKS = "books"
        const val KEY_HIGHLIGHTS = "highlights"
        const val KEY_READING_SESSIONS = "readingSessions"

        const val MAX_FILE_BYTES = 20 * 1024 * 1024
        const val MAX_BOOKS = 20_000
        const val MAX_CATEGORIES = 500
        const val MAX_HIGHLIGHTS = 200_000
        const val MAX_SESSIONS = 200_000
        const val MAX_SUBJECTS = 50
        const val MAX_TEXT = 200
        const val MAX_LONG_TEXT = 20_000
        const val MAX_SESSION_MS = 24 * 60 * 60 * 1000L
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val QUERY_CHUNK = 500
    }
}
