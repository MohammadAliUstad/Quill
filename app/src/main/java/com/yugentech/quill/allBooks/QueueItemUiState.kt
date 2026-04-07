package com.yugentech.quill.allBooks

data class QueueItemUiState(
    val bookId: String,
    val title: String,
    val coverUrl: String?,
    val isRunning: Boolean,
    val progress: Int
)