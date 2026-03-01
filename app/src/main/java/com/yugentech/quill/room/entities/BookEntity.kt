package com.yugentech.quill.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.quill.network.domain.BookSource
import com.yugentech.quill.room.daos.CategoryDao.Companion.SHELF
import kotlinx.serialization.Serializable

@Entity(tableName = "books")
data class BookEntity(
    // IDENTITY
    @PrimaryKey(autoGenerate = false)
    val id: String,

    // LIBRARY
    val title: String,
    val author: String,
    val coverUrl: String?,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val isFavorite: Boolean = false,
    val userCategory: String = SHELF,
    val addedAt: Long = System.currentTimeMillis(),
    val progressPercent: Float = 0f,
    val totalPages: Int = 0,
    val lastChapterTitle: String? = null,
    val lastReadTime: Long = 0,
    val lastChapterIndex: Int = 0,
    val lastScrollPosition: Int = 0,
    val lastLocatorJson: String? = null,

    // --- NEW: STORAGE MANAGEMENT ---
    val fileSizeBytes: Long = 0L,

    // BOOK DETAILS
    val description: String? = null,
    val subjects: List<String> = emptyList(),
    val language: String? = null,
    val downloadUrl: String,
    val source: BookSource,
    val localFilePath: String? = null,
    val chapters: List<Chapter> = emptyList()
)

@Serializable
data class Chapter(
    val title: String,
    val href: String,
    val index: Int,
    val depth: Int = 0,
    val pageCount: Int = 0
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}