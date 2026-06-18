package com.yugentech.quill.database.mapper

import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.database.view.LibraryBookView

fun Book.toEntity(
    isFavorite: Boolean = this.isFavorite,
    category: String = this.userCategory ?: "Shelf",
    status: DownloadStatus = this.downloadStatus,
    error: String? = this.downloadError
): BookEntity {
    return BookEntity(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl,
        downloadUrl = this.downloadUrl,
        source = this.source,
        description = this.description,
        subjects = this.subjects,
        language = this.language,
        downloadStatus = status,
        downloadError = error,
        isFavorite = isFavorite,
        userCategory = category,
        addedAt = System.currentTimeMillis(),
        progressPercent = this.progressPercent,
        totalPages = this.totalPages,
        lastReadTime = this.lastReadTime,
        lastChapterTitle = this.lastChapterTitle,
        lastChapterIndex = this.lastChapterIndex,
        lastScrollPosition = this.lastScrollPosition,
        lastLocatorJson = null,
        fileSizeBytes = 0L,
        chapters = this.chapters
    )
}

fun BookEntity.toDomainModel(): Book {
    return Book(
        id = this.id,
        title = this.title,
        author = this.author,
        description = this.description,
        coverUrl = this.coverUrl,
        downloadUrl = this.downloadUrl,
        source = this.source,
        subjects = this.subjects,
        language = this.language ?: "en",
        localFilePath = this.localFilePath,
        isFavorite = this.isFavorite,
        downloadStatus = this.downloadStatus,
        downloadError = this.downloadError,
        userCategory = this.userCategory,
        progressPercent = this.progressPercent,
        totalPages = this.totalPages,
        lastReadTime = this.lastReadTime,
        lastChapterTitle = this.lastChapterTitle,
        lastChapterIndex = this.lastChapterIndex,
        lastScrollPosition = this.lastScrollPosition,
        chapters = this.chapters
    )
}

fun LibraryBookView.toBook(): Book {
    return Book(
        id = this.id,
        title = this.title,
        author = this.author,
        coverUrl = this.coverUrl ?: "",
        downloadUrl = "",
        description = null,
        subjects = emptyList(),
        language = "",
        source = BookSource.STANDARD_EBOOKS,
        isFavorite = this.isFavorite,
        downloadStatus = this.downloadStatus,
        downloadError = this.downloadError,
        userCategory = this.userCategory,
        progressPercent = this.progressPercent,
        totalPages = this.totalPages,
        lastReadTime = this.lastReadTime,
        lastChapterTitle = null,
        lastChapterIndex = 0,
        lastScrollPosition = 0
    )
}
