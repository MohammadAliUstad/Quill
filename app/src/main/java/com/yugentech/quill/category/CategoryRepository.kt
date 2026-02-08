package com.yugentech.quill.category

import com.yugentech.quill.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<UserCategoryEntity>>
    fun getUserCategories(): Flow<List<UserCategoryEntity>>  // NEW: Excludes system categories
    suspend fun createCategory(name: String): Result<Unit>  // UPDATED: Validation built-in
    suspend fun deleteCategory(name: String)
    suspend fun updateCategories(categories: List<UserCategoryEntity>) // For reordering
    suspend fun initializeDefaultCategories()  // UPDATED: Seeds Favorites & Uncategorized
}