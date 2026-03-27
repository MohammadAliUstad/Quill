package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus

@Entity(
    tableName = "books"
)
data class BookEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val isFavorite: Boolean = false,
    val userCategory: String = "Shelf",
    val addedAt: Long = System.currentTimeMillis(),
    val progressPercent: Float = 0f,
    val totalPages: Int = 0,
    val lastChapterTitle: String? = null,
    val lastReadTime: Long = 0,
    val lastChapterIndex: Int = 0,
    val lastScrollPosition: Int = 0,
    val lastLocatorJson: String? = null,
    val spoilerLockEnabled: Boolean = true,
    val fileSizeBytes: Long = 0L,
    val isSynced: Boolean = false,
    val description: String? = null,
    val subjects: List<String> = emptyList(),
    val language: String? = null,
    val downloadUrl: String,
    val source: BookSource,
    val localFilePath: String? = null,
    val chapters: List<Chapter> = emptyList()
)