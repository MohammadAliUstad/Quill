package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_indexing_state")
data class BookIndexingStateEntity(
    @PrimaryKey val bookId: String,
    val lastCompletedChapterIndex: Int = -1,
    val isComplete: Boolean = false
)