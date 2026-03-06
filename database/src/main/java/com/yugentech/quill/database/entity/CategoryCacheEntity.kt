package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_cache"
)
data class CategoryCacheEntity(
    @PrimaryKey val name: String,
    val source: String, // "standard" or "gutenberg" — lets you reuse the table for both
    val cachedAt: Long = System.currentTimeMillis()
)