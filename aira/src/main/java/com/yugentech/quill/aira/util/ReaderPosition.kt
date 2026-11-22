package com.yugentech.quill.aira.util

import com.yugentech.quill.database.entity.BookEntity

/**
 * Where the reader is in the book, sent with each question so Aira can talk about the story "so
 * far" and knows what's still ahead of them.
 */
data class ReaderPosition(
    val chapterTitle: String?,
    /** 0-1, as the book stores it. */
    val progressPercent: Float,
    val spoilerLock: Boolean
) {
    fun toMap(): Map<String, Any> = buildMap {
        chapterTitle?.takeIf { it.isNotBlank() }?.let { put("chapterTitle", it) }
        if (progressPercent > 0f) put("progressPercent", progressPercent.coerceIn(0f, 1f))
        put("spoilerLock", spoilerLock)
    }

    companion object {
        fun of(book: BookEntity) = ReaderPosition(
            chapterTitle = book.lastChapterTitle,
            progressPercent = book.progressPercent,
            spoilerLock = book.spoilerLockEnabled
        )
    }
}
