package com.yugentech.quill.category

import com.yugentech.quill.room.daos.CategoryDao
import com.yugentech.quill.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<UserCategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override fun getUserCategories(): Flow<List<UserCategoryEntity>> {
        // Returns only user-created categories (excludes Favorites & Uncategorized)
        return categoryDao.getUserCategories()
    }

    override suspend fun createCategory(name: String): Result<Unit> {
        return try {
            // Validate category name
            if (name.isBlank()) {
                return Result.failure(Exception("Category name cannot be empty"))
            }

            // Check for reserved names
            if (name == "Favorites" || name == "Uncategorized") {
                return Result.failure(Exception("This is a reserved category name"))
            }

            // Get current max sortOrder for user categories (between 2-998)
            val userCategories = categoryDao.getUserCategories().first()
            val maxOrder = userCategories.maxOfOrNull { it.sortOrder } ?: 1

            val newCategory = UserCategoryEntity(
                name = name,
                sortOrder = maxOrder + 1,
                isSystem = false
            )

            categoryDao.insertCategory(newCategory)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(name: String) {
        // Uses the Transaction: moves books to "Uncategorized", then deletes category
        categoryDao.deleteCategoryAndResetBooks(name)
    }

    override suspend fun updateCategories(categories: List<UserCategoryEntity>) {
        // For drag-and-drop reordering
        categoryDao.updateCategories(categories)
    }

    override suspend fun initializeDefaultCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            // Seed system categories
            categoryDao.insertCategory(
                UserCategoryEntity(
                    name = "Favorites",
                    sortOrder = 1,
                    isSystem = true
                )
            )
            categoryDao.insertCategory(
                UserCategoryEntity(
                    name = "Uncategorized",
                    sortOrder = 999,
                    isSystem = true
                )
            )
        }
    }
}