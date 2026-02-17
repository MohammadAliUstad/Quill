package com.yugentech.quill.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val isSystem: Boolean = false
)