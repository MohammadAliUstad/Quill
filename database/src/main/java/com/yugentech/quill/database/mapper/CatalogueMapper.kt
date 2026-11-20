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

/**
 * A downloaded feed as cache rows, in the feed's own order. The cache lists by [cachedAt], so
 * each row is stamped one millisecond after the one before it: stamping them all with the same
 * moment left the order of ties to the database, and a refresh could shuffle the covers.
 */
fun List<Book>.toCatalogEntities(categorySlug: String): List<CatalogCacheEntity> {
    val start = System.currentTimeMillis()
    return mapIndexed { index, book -> book.toCatalogEntity(categorySlug).copy(cachedAt = start + index) }
}
