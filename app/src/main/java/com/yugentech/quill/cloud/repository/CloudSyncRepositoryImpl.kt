package com.yugentech.quill.cloud.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yugentech.quill.utils.EpubParser
import com.yugentech.quill.cloud.service.CloudSyncService
import com.yugentech.quill.cloud.worker.SyncWorker
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class CloudSyncRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val cloudSyncService: CloudSyncService,
    private val context: Context,
    private val authRepository: AuthRepository
) : CloudSyncRepository {

    // A guest's library stays on this phone: Firestore is never touched in guest mode
    private val isGuest: Boolean
        get() = authRepository.isGuest

    override suspend fun syncLibraryOnLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {

            bookDao.deleteAllBooks()
            categoryDao.deleteAllUserCategories()

            val cloudCategoriesResult = cloudSyncService.fetchCategories()
            if (cloudCategoriesResult.isSuccess) {
                val cloudCategories = cloudCategoriesResult.getOrNull() ?: emptyList()
                val categoryEntities = cloudCategories.map { categoryMap ->
                    CategoryEntity(
                        id = (categoryMap["id"] as? String)?.toLongOrNull() ?: 0L,
                        name = categoryMap["name"] as? String ?: "Unknown",
                        sortOrder = (categoryMap["sortOrder"] as? Long)?.toInt() ?: 0,
                        isSystem = (categoryMap["isSystem"] as? Boolean) ?: false,
                        isSynced = true
                    )
                }

                if (categoryEntities.isNotEmpty()) {
                    categoryDao.insertCategories(categoryEntities)
                }
            } else {
                Timber.e("Failed to fetch categories from cloud")
            }

            val cloudLibraryResult = cloudSyncService.fetchCloudLibrary()

            if (cloudLibraryResult.isSuccess) {
                val cloudBooks = cloudLibraryResult.getOrNull() ?: emptyList()
                val entitiesToInsert = mutableListOf<BookEntity>()

                for (cloudData in cloudBooks) {
                    val bookId = cloudData["id"] as? String ?: continue
                    // Project Gutenberg is no longer a source: its books can't be fetched, and
                    // any kept on this device are now imports (MIGRATION_3_4), which this mustn't
                    // overwrite
                    if (cloudData["source"] == LEGACY_GUTENBERG_SOURCE) continue

                    val expectedFile = File(context.filesDir, "books/$bookId.epub")
                    val fileExists = expectedFile.exists()

                    @Suppress("UNCHECKED_CAST")
                    val cloudSubjects = cloudData["subjects"] as? List<String> ?: emptyList()
                    val bookTitle = cloudData["title"] as? String ?: "Unknown Title"

                    var localChapters: List<Chapter> = emptyList()
                    var localTotalPages = (cloudData["totalPages"] as? Long)?.toInt() ?: 0

                    if (fileExists) {
                        try {
                            val parser = EpubParser(context)
                            val parsedData = parser.parse(expectedFile.absolutePath, bookTitle)
                            localChapters = parsedData.chapters
                            localTotalPages = parsedData.totalPages
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse a synced book")
                        }
                    }

                    val entity = BookEntity(
                        id = bookId,
                        title = bookTitle,
                        author = cloudData["author"] as? String ?: "Unknown Author",
                        coverUrl = cloudData["coverUrl"] as? String,
                        source = runCatching {
                            BookSource.valueOf(cloudData["source"] as String)
                        }.getOrDefault(BookSource.STANDARD_EBOOKS),
                        progressPercent = (cloudData["progressPercent"] as? Double)?.toFloat()
                            ?: 0f,
                        totalPages = localTotalPages,
                        lastChapterTitle = cloudData["lastChapterTitle"] as? String,
                        lastReadTime = cloudData["lastReadTime"] as? Long ?: 0L,
                        lastChapterIndex = (cloudData["lastChapterIndex"] as? Long)?.toInt() ?: 0,
                        lastScrollPosition = (cloudData["lastScrollPosition"] as? Long)?.toInt()
                            ?: 0,
                        lastLocatorJson = cloudData["lastLocatorJson"] as? String,
                        userCategory = cloudData["userCategory"] as? String ?: SHELF,
                        isFavorite = cloudData["isFavorite"] as? Boolean ?: false,
                        downloadUrl = cloudData["downloadUrl"] as? String ?: EMPTY,
                        description = cloudData["description"] as? String,
                        subjects = cloudSubjects,
                        language = cloudData["language"] as? String,
                        downloadStatus = if (fileExists) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
                        localFilePath = if (fileExists) expectedFile.absolutePath else null,
                        fileSizeBytes = if (fileExists) expectedFile.length() else 0L,
                        chapters = localChapters
                    )

                    entitiesToInsert.add(entity)
                }

                bookDao.insertBooks(entitiesToInsert)
                Result.success(Unit)
            } else {
                Timber.e("Failed to fetch library from cloud")
                Result.failure(
                    cloudLibraryResult.exceptionOrNull() ?: Exception("Unknown sync error")
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during library sync")
            Result.failure(e)
        }
    }

    override suspend fun wipeLocalData() = withContext(Dispatchers.IO) {
        bookDao.deleteAllBooks()
        categoryDao.deleteAllUserCategories()
    }

    override suspend fun syncBooksToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isGuest) return@withContext Result.success(Unit)
        try {
            val unsyncedBooks = bookDao.getUnsyncedBooks()

            val booksToSync = unsyncedBooks.filter { it.source != BookSource.USER_IMPORTED }

            if (booksToSync.isEmpty()) {
                bookDao.markAllBooksAsSynced()
                return@withContext Result.success(Unit)
            }

            booksToSync.forEach { book ->
                cloudSyncService.syncBookToCloud(book)
            }

            bookDao.markAllBooksAsSynced()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload books to cloud")
            Result.failure(e)
        }
    }

    override suspend fun syncCategoriesToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isGuest) return@withContext Result.success(Unit)
        try {
            val unsyncedCategories = categoryDao.getUnsyncedCategories()

            if (unsyncedCategories.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            unsyncedCategories.forEach { category ->
                cloudSyncService.syncCategoryToCloud(category)
            }

            categoryDao.markAllAsSynced()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload categories to cloud")
            Result.failure(e)
        }
    }

    override suspend fun deleteCategoryFromCloud(categoryId: String) = withContext(Dispatchers.IO) {
        if (isGuest) return@withContext
        try {
            cloudSyncService.deleteCategoryFromCloud(categoryId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete category from cloud")
        }
    }

    override suspend fun deleteBookFromCloud(bookId: String) = withContext(Dispatchers.IO) {
        if (isGuest) return@withContext
        try {
            cloudSyncService.deleteBookFromCloud(bookId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete book from cloud")
        }
    }

    override fun scheduleBackgroundSync() {
        if (isGuest) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "OpportunisticCloudSync",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    override suspend fun syncCategoriesOnLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {

            bookDao.deleteAllBooks()
            categoryDao.deleteAllUserCategories()

            val cloudCategoriesResult = cloudSyncService.fetchCategories()

            if (cloudCategoriesResult.isSuccess) {
                val cloudCategories = cloudCategoriesResult.getOrNull() ?: emptyList()

                val categoryEntities = cloudCategories.map { categoryMap ->
                    CategoryEntity(
                        id = (categoryMap["id"] as? String)?.toLongOrNull() ?: 0L,
                        name = categoryMap["name"] as? String ?: "Unknown",
                        sortOrder = (categoryMap["sortOrder"] as? Long)?.toInt() ?: 0,
                        isSystem = (categoryMap["isSystem"] as? Boolean) ?: false,
                        isSynced = true
                    )
                }

                if (categoryEntities.isNotEmpty()) {
                    categoryDao.insertCategories(categoryEntities)
                }

                Result.success(Unit)
            } else {
                val error =
                    cloudCategoriesResult.exceptionOrNull() ?: Exception("Cloud fetch failed")
                Timber.e(error, "Failed to fetch categories from Firestore")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during category login sync")
            Result.failure(e)
        }
    }
}

/** What Project Gutenberg books were saved as in the cloud, before it stopped being a source. */
private const val LEGACY_GUTENBERG_SOURCE = "GUTENBERG"
