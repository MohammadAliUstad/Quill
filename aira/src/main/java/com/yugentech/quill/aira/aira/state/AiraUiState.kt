package com.yugentech.quill.aira.aira.state

import com.yugentech.quill.aira.aira.message.AiraMessage

data class AiraUiState(
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val lastChapterTitle: String? = null,
    val isReady: Boolean = false,
    val isIndexing: Boolean = true,
    val indexingProgress: Int = 0,
    val hasStartedReading: Boolean = false,
    val messages: List<AiraMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val spoilerLockEnabled: Boolean = true,
    val error: String? = null,
    val canSendQuery: Boolean = true,
    val remainingQueries: Int = 10,
    val showPaywall: Boolean = false,
    val isPro: Boolean = false
)