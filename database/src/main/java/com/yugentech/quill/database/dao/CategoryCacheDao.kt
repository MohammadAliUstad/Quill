package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * Swaps [source]'s cached categories for [categories] in one transaction, so the chip row
     * sees one change. As two writes, it was announced empty in between and the row collapsed
     * to New Arrivals before filling back up.
     */
    @Transaction
    suspend fun replaceCategories(source: String, categories: List<CategoryCacheEntity>) {
        clearCategories(source)
        insertCategories(categories)
    }
}