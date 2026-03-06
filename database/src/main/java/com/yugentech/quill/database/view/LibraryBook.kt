package com.yugentech.quill.database.view

import androidx.room.DatabaseView
import com.yugentech.quill.database.model.DownloadStatus

@DatabaseView(
    viewName = "library_view",
    value = """
        SELECT id, title, author, coverUrl, downloadStatus, isFavorite, 
               userCategory, progressPercent, lastReadTime, addedAt,
               totalPages, lastChapterTitle 
        FROM books
    """
)
data class LibraryBookView(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val downloadStatus: DownloadStatus,
    val isFavorite: Boolean,
    val userCategory: String,
    val progressPercent: Float,
    val lastReadTime: Long,
    val addedAt: Long,
    val totalPages: Int,
    val lastChapterTitle: String?
)