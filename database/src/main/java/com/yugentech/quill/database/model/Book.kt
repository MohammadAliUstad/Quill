package com.yugentech.quill.database.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String?,
    val coverUrl: String?,
    val downloadUrl: String,
    val source: BookSource,
    val subjects: List<String>,
    val language: String,
    val localFilePath: String? = null,
    val isFavorite: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val userCategory: String? = null,
    val progressPercent: Float = 0f,
    val totalPages: Int = 0,
    val lastReadTime: Long = 0,
    val lastChapterTitle: String? = null,
    val lastChapterIndex: Int = 0,
    val lastScrollPosition: Int = 0,
    val chapters: List<Chapter> = emptyList()
)