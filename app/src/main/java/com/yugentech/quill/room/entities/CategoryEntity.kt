package com.yugentech.quill.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.quill.category.CategoryModel

@Entity(tableName = "user_categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val isSystem: Boolean = false
)

fun CategoryEntity.toDomain(): CategoryModel {
    return CategoryModel(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder
    )
}