package com.yugentech.quill.ui.info.indexing.state

data class IndexingUiState(
    val bookId: String,
    val title: String,
    val coverUrl: String?,
    val isRunning: Boolean,
    val progress: Int
)