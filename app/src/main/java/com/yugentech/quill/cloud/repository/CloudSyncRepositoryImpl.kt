package com.yugentech.quill.cloud.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yugentech.quill.bookDetails.EpubParser
import com.yugentech.quill.cloud.CloudSyncService
import com.yugentech.quill.cloud.worker.SyncWorker
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class CloudSyncRepositoryImpl(
    private val bookDao: BookDao,
    private val categoryDao: CategoryDao,
    private val cloudSyncService: CloudSyncService,
    private val context: Context
) : CloudSyncRepository {

    override suspend fun syncLibraryOnLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting library and category sync on login")

            // 1. Wipe the local database
            bookDao.deleteAllBooks()
            categoryDao.deleteAllUserCategories()

            // 2. Fetch and restore the user's categories from Firestore first
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
                    Timber.i("Successfully synced ${categoryEntities.size} categories to local database")
                }
            } else {
                Timber.e("Failed to fetch categories from cloud")
            }

            // 3. Fetch the user's library from Firestore
            val cloudLibraryResult = cloudSyncService.fetchCloudLibrary()

            if (cloudLibraryResult.isSuccess) {
                val cloudBooks = cloudLibraryResult.getOrNull() ?: emptyList()
                val entitiesToInsert = mutableListOf<BookEntity>()

                for (cloudData in cloudBooks) {
                    val bookId = cloudData["id"] as? String ?: continue

                    // CHANGED: Point to the internal hidden books folder
                    val expectedFile = File(context.filesDir, "books/$bookId.epub")
                    val fileExists = expectedFile.exists()

                    @Suppress("UNCHECKED_CAST")
                    val cloudSubjects = cloudData["subjects"] as? List<String> ?: emptyList()
                    val bookTitle = cloudData["title"] as? String ?: "Unknown Title"

                    // --- NEW: The Silent Re-Parse ---
                    var localChapters: List<Chapter> = emptyList()
                    var localTotalPages = (cloudData["totalPages"] as? Long)?.toInt() ?: 0

                    if (fileExists) {
                        try {
                            // If the file is here, parse it quickly to rebuild the Table of Contents
                            val parser = EpubParser(context)
                            val parsedData = parser.parse(expectedFile.absolutePath, bookTitle)
                            localChapters = parsedData.chapters
                            localTotalPages = parsedData.totalPages // Use accurate local calculation
                        } catch (e: Exception) {
                            Timber.e(e, "Silent parse failed for existing book: $bookId")
                        }
                    }
                    // --------------------------------

                    val entity = BookEntity(
                        id = bookId,
                        title = bookTitle,
                        author = cloudData["author"] as? String ?: "Unknown Author",
                        coverUrl = cloudData["coverUrl"] as? String,
                        source = runCatching {
                            BookSource.valueOf(cloudData["source"] as String)
                        }.getOrDefault(BookSource.STANDARD_EBOOKS),

                        progressPercent = (cloudData["progressPercent"] as? Double)?.toFloat() ?: 0f,

                        // CHANGED: Use the local reparsed pages if available, otherwise cloud fallback
                        totalPages = localTotalPages,

                        lastChapterTitle = cloudData["lastChapterTitle"] as? String,
                        lastReadTime = cloudData["lastReadTime"] as? Long ?: 0L,
                        lastChapterIndex = (cloudData["lastChapterIndex"] as? Long)?.toInt() ?: 0,
                        lastScrollPosition = (cloudData["lastScrollPosition"] as? Long)?.toInt() ?: 0,
                        lastLocatorJson = cloudData["lastLocatorJson"] as? String,
                        userCategory = cloudData["userCategory"] as? String ?: "Shelf",
                        isFavorite = cloudData["isFavorite"] as? Boolean ?: false,
                        downloadUrl = cloudData["downloadUrl"] as? String ?: "",
                        description = cloudData["description"] as? String,
                        subjects = cloudSubjects,
                        language = cloudData["language"] as? String,

                        downloadStatus = if (fileExists) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
                        localFilePath = if (fileExists) expectedFile.absolutePath else null,
                        fileSizeBytes = if (fileExists) expectedFile.length() else 0L,

                        // CHANGED: Attach the rebuilt chapter list!
                        chapters = localChapters
                    )

                    entitiesToInsert.add(entity)
                }

                bookDao.insertBooks(entitiesToInsert)
                Timber.i("Successfully synced ${entitiesToInsert.size} books to local database")
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
        Timber.d("Wiping local data on sign out")
        bookDao.deleteAllBooks()
        categoryDao.deleteAllUserCategories()
        // Clear sessions and Aira messages here too
    }

    override suspend fun syncBooksToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Ask the DAO for the dirty books
            val unsyncedBooks = bookDao.getUnsyncedBooks()

            if (unsyncedBooks.isEmpty()) {
                Timber.d("No unsynced books to upload")
                return@withContext Result.success(Unit)
            }

            // 2. Upload them one by one
            unsyncedBooks.forEach { book ->
                cloudSyncService.syncBookToCloud(book)
            }

            // 3. Mark them all as clean!
            bookDao.markAllBooksAsSynced()

            Timber.i("Successfully uploaded ${unsyncedBooks.size} books to cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload books to cloud")
            Result.failure(e)
        }
    }

    override suspend fun syncCategoriesToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unsyncedCategories = categoryDao.getUnsyncedCategories()

            if (unsyncedCategories.isEmpty()) {
                Timber.d("No unsynced categories to upload")
                return@withContext Result.success(Unit)
            }

            unsyncedCategories.forEach { category ->
                cloudSyncService.syncCategoryToCloud(category)
            }

            categoryDao.markAllAsSynced()
            Timber.i("Successfully uploaded ${unsyncedCategories.size} categories to cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload categories to cloud")
            Result.failure(e)
        }
    }

    override suspend fun deleteCategoryFromCloud(categoryId: String) = withContext(Dispatchers.IO) {
        try {
            // ONLY handles the network call
            cloudSyncService.deleteCategoryFromCloud(categoryId)
            Timber.i("Successfully deleted category document $categoryId from cloud")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete category $categoryId from cloud")
        }
    }

    override suspend fun deleteBookFromCloud(bookId: String) = withContext(Dispatchers.IO) {
        try {
            // ONLY handles the network call to Firestore
            cloudSyncService.deleteBookFromCloud(bookId)
            Timber.i("Successfully deleted book document $bookId from cloud")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete book $bookId from cloud")
        }
    }

    override fun scheduleBackgroundSync() {
        Timber.d("Scheduling background sync in 15 seconds")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            // Wait 15 seconds before actually running the network call
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        // REPLACE is the magic policy that resets the timer
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OpportunisticCloudSync",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }

    override suspend fun syncCategoriesOnLogin(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting category-only sync on login")

            // 1. Wipe the local database to ensure a clean slate for the new user
            bookDao.deleteAllBooks()
            categoryDao.deleteAllUserCategories()

            // 2. Fetch the user's categories from Firestore
            val cloudCategoriesResult = cloudSyncService.fetchCategories()

            if (cloudCategoriesResult.isSuccess) {
                val cloudCategories = cloudCategoriesResult.getOrNull() ?: emptyList()

                // 3. Map Firestore maps back to Room entities
                val categoryEntities = cloudCategories.map { categoryMap ->
                    CategoryEntity(
                        // Firestore stores the Long ID as a String
                        id = (categoryMap["id"] as? String)?.toLongOrNull() ?: 0L,
                        name = categoryMap["name"] as? String ?: "Unknown",
                        sortOrder = (categoryMap["sortOrder"] as? Long)?.toInt() ?: 0,
                        isSystem = (categoryMap["isSystem"] as? Boolean) ?: false,
                        isSynced = true // Freshly pulled from the cloud
                    )
                }

                if (categoryEntities.isNotEmpty()) {
                    // 4. Batch insert into Room
                    categoryDao.insertCategories(categoryEntities)
                    Timber.i("Successfully restored ${categoryEntities.size} categories")
                }

                Result.success(Unit)
            } else {
                val error = cloudCategoriesResult.exceptionOrNull() ?: Exception("Cloud fetch failed")
                Timber.e(error, "Failed to fetch categories from Firestore")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during category login sync")
            Result.failure(e)
        }
    }
}