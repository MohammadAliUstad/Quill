package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yugentech.quill.database.model.RetrievedChunk

@Entity(
    tableName = "aira_messages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class AiraMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val role: AiraMessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<RetrievedChunk> = emptyList()
)

enum class AiraMessageRole {
    USER, AIRA
}