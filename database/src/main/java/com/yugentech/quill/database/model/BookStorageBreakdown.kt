package com.yugentech.quill.database.model

data class BookStorageBreakdown(
    val bookId: String,
    val fileSizeBytes: Long,
    val chunksBytes: Long,
    val messagesBytes: Long
) {
    val totalBytes: Long
        get() = fileSizeBytes + chunksBytes + (chunksBytes / 10) + messagesBytes
}