package com.yugentech.quill.database.model

// BookStorageBreakdown.kt
data class BookStorageBreakdown(
    val bookId: String,
    val fileSizeBytes: Long,
    val chunksBytes: Long,      // text + embeddings
    val messagesBytes: Long
) {
    val totalBytes: Long
        get() = fileSizeBytes + chunksBytes + (chunksBytes / 10) + messagesBytes
        //                                    ^^^ ~10% FTS overhead approximation
}