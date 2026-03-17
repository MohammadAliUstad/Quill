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

    // Identifies categories modified locally that need to be uploaded to Firestore.
    @Query("SELECT * FROM user_categories WHERE isSynced = 0")
    abstract suspend fun getUnsyncedCategories(): List<CategoryEntity>

    // Updates all local categories to a synced state after a successful cloud upload.
    @Query("UPDATE user_categories SET isSynced = 1 WHERE isSynced = 0")
    abstract suspend fun markAllAsSynced()

    // Streams all categories including system-defined ones sorted by order.
    @Query("SELECT * FROM user_categories ORDER BY sortOrder ASC")
    abstract fun getAllCategories(): Flow<List<CategoryEntity>>

    // Streams only the custom shelves created by the user.
    @Query("SELECT * FROM user_categories WHERE isSystem = 0 ORDER BY sortOrder ASC")
    abstract fun getUserCategories(): Flow<List<CategoryEntity>>

    // Returns the total number of categories currently in the database.
    @Query("SELECT COUNT(*) FROM user_categories")
    abstract suspend fun getCategoryCount(): Int

    // Adds a new category only if it does not already exist.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCategory(category: CategoryEntity)

    // Inserts or overwrites a list of categories, typically used during cloud restoration.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    // Updates the properties of a single existing category.
    @Update
    abstract suspend fun updateCategory(category: CategoryEntity)

    // Updates multiple categories at once, useful for reordering the shelf list.
    @Update
    abstract suspend fun updateCategories(categories: List<CategoryEntity>)

    // Orchestrates a category deletion by reassigning affected books before removal.
    @Transaction
    open suspend fun deleteCategory(name: String) {
        moveBooksToDefault(name)
        removeCategory(name)
    }

    // Reassigns books from a deleted category back to the default system shelf.
    @Query("UPDATE books SET userCategory = :defaultCategory WHERE userCategory = :oldCategory")
    protected abstract suspend fun moveBooksToDefault(
        oldCategory: String,
        defaultCategory: String = "Shelf"
    )

    // Removes a specific user-created category from the database by name.
    @Query("DELETE FROM user_categories WHERE name = :name AND isSystem = 0")
    protected abstract suspend fun removeCategory(name: String)

    // Clears all custom user shelves while preserving system categories.
    @Query("DELETE FROM user_categories WHERE isSystem = 0")
    abstract suspend fun deleteAllUserCategories()
}