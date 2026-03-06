package com.yugentech.quill.aira.aira

// In AiraUiState.kt
data class AiraUiState(
    val messages: List<AiraMessage> = emptyList(),
    val isLoading: Boolean = false, // True during RAG/Search phase
    val isStreaming: Boolean = false, // Add this: True while Gemini is typing
    val isReady: Boolean = false,
    val isIndexing: Boolean = false,
    val spoilerLockEnabled: Boolean = false,
    val error: String? = null
)