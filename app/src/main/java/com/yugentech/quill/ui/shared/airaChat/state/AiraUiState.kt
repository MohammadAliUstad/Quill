package com.yugentech.quill.ui.shared.airaChat.state

import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.quota.model.QuotaLimits
import com.yugentech.theme.tokens.AppConstants.EMPTY

data class AiraUiState(
    val bookTitle: String = EMPTY,
    val bookAuthor: String = EMPTY,
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
    val remainingQueries: Int = QuotaLimits.FREE,
    val showPaywall: Boolean = false,
    val isPro: Boolean = false
)
