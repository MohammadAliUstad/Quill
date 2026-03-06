package com.yugentech.quill.category.repository

import com.yugentech.quill.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    // Reads
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getUserCategories(): Flow<List<CategoryEntity>>
    suspend fun getCategoryCount(): Int

    // Writes
    suspend fun initializeDefaultCategories()
    suspend fun insertCategory(name: String)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun updateCategories(categories: List<CategoryEntity>)

    // Deletion
    suspend fun deleteCategory(name: String)
}