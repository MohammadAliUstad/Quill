package com.yugentech.quill.reader.state

data class QuickUiState(
    val isReady: Boolean = false,
    val isPro: Boolean = false,
    val response: String? = null,
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val canSendQuery: Boolean = true,
    val showPaywall: Boolean = false,
    val error: String? = null
)
