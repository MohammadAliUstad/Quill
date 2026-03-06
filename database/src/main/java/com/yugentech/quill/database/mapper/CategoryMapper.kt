package com.yugentech.quill.database.mapper

import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.model.Category

fun CategoryEntity.toDomainModel(): Category {
    return Category(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder
    )
}

fun Category.toEntity(isSystem: Boolean = false): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        name = this.name,
        sortOrder = this.sortOrder,
        isSystem = isSystem
    )
}