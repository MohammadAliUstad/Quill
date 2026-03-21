package com.yugentech.quill.aira.aira.viewmodel

data class AiraUiState(
    // Book Metadata
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val lastChapterTitle: String? = null,

    // Chat State
    val isReady: Boolean = false,
    val isIndexing: Boolean = false,
    val hasStartedReading: Boolean = false,
    val messages: List<AiraMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val spoilerLockEnabled: Boolean = true,
    val error: String? = null,

    // Quota State
    val canSendQuery: Boolean = true,
    val remainingQueries: Int = 10,
    val showPaywall: Boolean = false
)