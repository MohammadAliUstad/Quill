package com.yugentech.quill.category.repository

import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.theme.tokens.AppConstants.SHELF
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
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
            isSystem = true
        )
        categoryDao.insertCategory(systemShelf)
    }

    override suspend fun insertCategory(name: String) {
        val entity = CategoryEntity(
            name = name,
            sortOrder = categoryDao.getCategoryCount(),
            isSystem = false
        )
        categoryDao.insertCategory(entity)
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    override suspend fun updateCategories(categories: List<CategoryEntity>) {
        categoryDao.updateCategories(categories)
    }

    // Deletion
    override suspend fun deleteCategory(name: String) {
        categoryDao.deleteCategory(name)
    }
}