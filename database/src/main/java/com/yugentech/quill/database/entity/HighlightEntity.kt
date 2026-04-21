package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val bookId: String,
    val locatorJson: String,
    val colorInt: Int,
    val note: String? = null,
    val style: String = "HIGHLIGHT",
    val createdAt: Long = System.currentTimeMillis()
)