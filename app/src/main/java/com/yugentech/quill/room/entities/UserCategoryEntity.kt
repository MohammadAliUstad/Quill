package com.yugentech.quill.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int,
    val isSystem: Boolean = false  // true for Favorites, Uncategorized (non-deletable)
)