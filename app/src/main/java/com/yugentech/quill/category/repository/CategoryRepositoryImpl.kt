package com.yugentech.quill.category.repository

import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.cloud.repository.CloudSyncRepository // <-- New Import
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val cloudSyncRepository: CloudSyncRepository // <-- Injected Cloud Repo
) : CategoryRepository {

    // Reads
    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getUserCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getUserCategories()

    override suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    // Writes
    override suspend fun initializeDefaultCategories() {
        val systemShelf = CategoryEntity(
            name = SHELF,
            sortOrder = 99,
            isSystem = true,
            isSynced = true // System shelves are static and don't need cloud sync
        )
        categoryDao.insertCategory(systemShelf)
    }

    override suspend fun insertCategory(name: String) {
        val entity = CategoryEntity(
            name = name,
            sortOrder = categoryDao.getCategoryCount(),
            isSystem = false,
            isSynced = false // Mark as needing sync
        )
        categoryDao.insertCategory(entity)

        // Automatically schedule the debounced background sync
        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        // Ensure the dirty flag is set before updating Room
        categoryDao.updateCategory(category.copy(isSynced = false))

        // Automatically schedule the debounced background sync
        cloudSyncRepository.scheduleBackgroundSync()
    }

    override suspend fun updateCategories(categories: List<CategoryEntity>) {
        // Map the entire list (often used for reordering) to ensure all are marked unsynced
        val unsyncedList = categories.map { it.copy(isSynced = false) }
        categoryDao.updateCategories(unsyncedList)

        cloudSyncRepository.scheduleBackgroundSync()
    }

    // Deletion
    // Deletion
    override suspend fun deleteCategory(category: CategoryEntity) {
        // 1. Delete locally using the name
        categoryDao.deleteCategory(category.name)

        // 2. Delete from cloud using the unique ID
        cloudSyncRepository.deleteCategoryFromCloud(category.id.toString())
    }
}