package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.quill.database.model.BookSource

@Entity(
    tableName = "catalog_cache"
)
data class CatalogCacheEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val categorySlug: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val description: String?,
    val downloadUrl: String,
    val source: BookSource,
    val subjects: List<String>,
    val language: String
)