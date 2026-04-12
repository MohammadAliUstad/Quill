package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_cache"
)
data class CategoryCacheEntity(
    @PrimaryKey val name: String,
    val source: String,
    val cachedAt: Long = System.currentTimeMillis()
)