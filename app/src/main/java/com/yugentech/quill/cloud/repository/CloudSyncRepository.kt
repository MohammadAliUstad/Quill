package com.yugentech.quill.cloud.repository

interface CloudSyncRepository {
    suspend fun syncLibraryOnLogin(): Result<Unit>
    suspend fun syncCategoriesOnLogin(): Result<Unit>
    suspend fun wipeLocalData()
    suspend fun syncBooksToCloud(): Result<Unit>
    suspend fun syncCategoriesToCloud(): Result<Unit>
    suspend fun deleteBookFromCloud(bookId: String)
    suspend fun deleteCategoryFromCloud(categoryId: String)
    fun scheduleBackgroundSync()
}