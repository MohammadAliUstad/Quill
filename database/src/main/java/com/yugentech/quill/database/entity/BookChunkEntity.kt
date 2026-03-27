package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_chunks",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "chapterIndex"]),
        Index(value = ["bookId", "chapterIndex", "chunkIndex"])
    ]
)
data class BookChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String = "",  // spine link title for debugging
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray,
    val embeddingSize: Int = embedding.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BookChunkEntity) return false
        return id == other.id &&
                bookId == other.bookId &&
                chapterIndex == other.chapterIndex &&
                chapterTitle == other.chapterTitle &&
                chunkIndex == other.chunkIndex &&
                text == other.text &&
                embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + bookId.hashCode()
        result = 31 * result + chapterIndex
        result = 31 * result + chapterTitle.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}