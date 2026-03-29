package com.yugentech.quill.aira.quickPrompt.viewmodel

data class QuickChatUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val response: String? = null,
    val error: String? = null,
    val showPaywall: Boolean = false
)