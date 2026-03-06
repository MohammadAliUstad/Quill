package com.yugentech.quill.database.mapper

import com.yugentech.quill.database.entity.CatalogCacheEntity
import com.yugentech.quill.database.model.Book

fun CatalogCacheEntity.toDomainModel(): Book {
    return Book(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl ?: "",
        description = this.description ?: "",
        subjects = this.subjects,
        language = this.language,
        downloadUrl = this.downloadUrl,
        source = this.source,
        localFilePath = null
    )
}

fun Book.toCatalogEntity(categorySlug: String): CatalogCacheEntity {
    return CatalogCacheEntity(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl,
        categorySlug = categorySlug,
        description = this.description,
        downloadUrl = this.downloadUrl,
        source = this.source,
        subjects = this.subjects,
        language = this.language
    )
}