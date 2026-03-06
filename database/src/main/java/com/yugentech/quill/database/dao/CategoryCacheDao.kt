package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.CategoryCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryCacheDao {

    @Query("SELECT name FROM category_cache WHERE source = :source ORDER BY cachedAt ASC")
    fun getCategoriesBySource(source: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryCacheEntity>)

    @Query("DELETE FROM category_cache WHERE source = :source")
    suspend fun clearCategories(source: String)
}