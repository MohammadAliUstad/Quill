package com.yugentech.quill.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yugentech.quill.room.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    companion object {
        const val SHELF = "Shelf"
    }

    // Reads
    @Query("SELECT * FROM user_categories ORDER BY sortOrder ASC")
    abstract fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM user_categories WHERE isSystem = 0 ORDER BY sortOrder ASC")
    abstract fun getUserCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM user_categories")
    abstract suspend fun getCategoryCount(): Int

    // Writes
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCategory(category: CategoryEntity)

    @Update
    abstract suspend fun updateCategory(category: CategoryEntity)

    @Update
    abstract suspend fun updateCategories(categories: List<CategoryEntity>)

    // Deletion
    @Transaction
    open suspend fun deleteCategory(name: String) {
        moveBooksToDefault(name)
        removeCategory(name)
    }

    @Query("UPDATE books SET userCategory = :defaultCategory WHERE userCategory = :oldCategory")
    protected abstract suspend fun moveBooksToDefault(
        oldCategory: String,
        defaultCategory: String = SHELF
    )

    @Query("DELETE FROM user_categories WHERE name = :name AND isSystem = 0")
    protected abstract suspend fun removeCategory(name: String)
}