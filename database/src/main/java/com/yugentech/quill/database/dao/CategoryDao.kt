package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yugentech.quill.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    @Query("SELECT * FROM user_categories WHERE isSynced = 0")
    abstract suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("UPDATE user_categories SET isSynced = 1 WHERE isSynced = 0")
    abstract suspend fun markAllAsSynced()

    @Query("SELECT * FROM user_categories ORDER BY sortOrder ASC")
    abstract fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM user_categories WHERE isSystem = 0 ORDER BY sortOrder ASC")
    abstract fun getUserCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM user_categories")
    abstract suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    abstract suspend fun updateCategory(category: CategoryEntity)

    @Update
    abstract suspend fun updateCategories(categories: List<CategoryEntity>)

    @Transaction
    open suspend fun deleteCategory(name: String) {
        moveBooksToDefault(name)
        removeCategory(name)
    }

    @Query("UPDATE books SET userCategory = :defaultCategory WHERE userCategory = :oldCategory")
    protected abstract suspend fun moveBooksToDefault(
        oldCategory: String,
        defaultCategory: String = "Shelf"
    )

    @Query("DELETE FROM user_categories WHERE name = :name AND isSystem = 0")
    protected abstract suspend fun removeCategory(name: String)

    @Query("DELETE FROM user_categories WHERE isSystem = 0")
    abstract suspend fun deleteAllUserCategories()
}