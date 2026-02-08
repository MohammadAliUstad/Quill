package com.yugentech.quill.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yugentech.quill.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // --- 1. Get Categories ---
    @Query("SELECT * FROM user_categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<UserCategoryEntity>>

    // Get only user-created categories (exclude system categories)
    @Query("SELECT * FROM user_categories WHERE isSystem = 0 ORDER BY sortOrder ASC")
    fun getUserCategories(): Flow<List<UserCategoryEntity>>

    @Query("SELECT COUNT(*) FROM user_categories")
    suspend fun getCategoryCount(): Int

    // --- 2. Manage Categories ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: UserCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<UserCategoryEntity>)

    @Update
    suspend fun updateCategory(category: UserCategoryEntity)

    // For Drag-and-Drop Reordering
    @Update
    suspend fun updateCategories(categories: List<UserCategoryEntity>)

    // --- 3. Smart Delete ---
    @Query("DELETE FROM user_categories WHERE name = :name AND isSystem = 0")
    suspend fun deleteCategory(name: String)

    // Helper: Move books back to Uncategorized when deleting custom category
    @Query("UPDATE library_books SET userCategory = 'Uncategorized' WHERE userCategory = :oldCategory")
    suspend fun resetBooksCategory(oldCategory: String)

    @Transaction
    suspend fun deleteCategoryAndResetBooks(name: String) {
        resetBooksCategory(name)     // Move books to "Uncategorized"
        deleteCategory(name)          // Delete the custom category
    }

    // --- 4. Initialization Helper ---
    // Initialize default system categories on first app launch
    suspend fun initializeDefaultCategories() {
        insertCategories(
            listOf(
                UserCategoryEntity(name = "Favorites", sortOrder = 1, isSystem = true),
                UserCategoryEntity(name = "Uncategorized", sortOrder = 999, isSystem = true)
            )
        )
    }
}