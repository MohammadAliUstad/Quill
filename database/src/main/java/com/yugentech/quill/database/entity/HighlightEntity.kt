package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.yugentech.quill.database.model.HighlightStyle

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
    val style: HighlightStyle = HighlightStyle.HIGHLIGHT,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
