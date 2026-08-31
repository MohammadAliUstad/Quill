package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visuals",
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
data class VisualEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val sourceText: String,
    val imagePath: String,
    val locatorJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
