package com.yugentech.quill.aira.quick.state

data class QuickUiState(
    val isIndexed: Boolean = false,
    val response: String? = null,
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val canSendQuery: Boolean = true,
    val showPaywall: Boolean = false,
    val error: String? = null
)