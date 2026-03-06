package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.CatalogCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query("SELECT * FROM catalog_cache WHERE categorySlug = :category ORDER BY cachedAt ASC")
    fun getBooksByCategory(category: String): Flow<List<CatalogCacheEntity>>

    @Query("SELECT * FROM catalog_cache WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): CatalogCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<CatalogCacheEntity>)

    @Query("DELETE FROM catalog_cache WHERE categorySlug = :category")
    suspend fun clearCategory(category: String)

    // Insert-first pattern — avoids Flow emitting empty list between clear and insert
    @Query("DELETE FROM catalog_cache WHERE categorySlug = :category AND id NOT IN (:freshIds)")
    suspend fun deleteStaleBooks(category: String, freshIds: List<String>)
}