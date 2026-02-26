package com.yugentech.quill.category

import com.yugentech.quill.room.entities.CategoryEntity

data class CategoryModel(
    val id: Long,
    val name: String,
    val sortOrder: Int
)

fun CategoryModel.toEntity(isSystem: Boolean = false): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder,
        isSystem = isSystem
    )
}