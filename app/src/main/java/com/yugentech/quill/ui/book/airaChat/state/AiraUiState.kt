package com.yugentech.quill.ui.book.airaChat.state

import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.quota.model.QuotaLimits
import com.yugentech.theme.tokens.AppConstants.EMPTY

data class AiraUiState(
    val bookTitle: String = EMPTY,
    val bookAuthor: String = EMPTY,
    val lastChapterTitle: String? = null,
    val isReady: Boolean = false,
    // Reading the book now, or waiting its turn behind other books in the indexing queue
    val isIndexing: Boolean = false,
    val isQueued: Boolean = false,
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
    val isPro: Boolean = false,
    // Each of these is filled by its own async lookup when the screen opens. Until all three have
    // answered, the screen holds its content back instead of showing guessed defaults.
    val isBookLoaded: Boolean = false,
    val isIndexStatusLoaded: Boolean = false,
    val areMessagesLoaded: Boolean = false
) {
    val isInitialized: Boolean get() = isBookLoaded && isIndexStatusLoaded && areMessagesLoaded

    /** Aira can be asked: her index of the book is finished and the reader has started it. */
    val canChat: Boolean get() = isInitialized && isReady && hasStartedReading
}
