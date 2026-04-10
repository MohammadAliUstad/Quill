package com.yugentech.quill.viewmodel.indexing

data class IndexingUiState(
    val bookId: String,
    val title: String,
    val coverUrl: String?,
    val isRunning: Boolean,
    val progress: Int
)