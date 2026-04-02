package com.yugentech.quill.aira.aira.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.aira.message.AiraMessage
import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.aira.state.AiraUiState
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.theme.tokens.AppConstants.EMPTY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiraViewModel(
    val bookId: String,
    private val airaChatRepository: AiraChatRepository,
    private val bookRepository: BookRepository,
    private val quotaRepository: QuotaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiraUiState())
    val uiState = _uiState.asStateFlow()

    private var isCurrentlyStreaming = false
    private var generationJob: Job? = null

    private val currentUserId: String? get() = authRepository.currentUser

    init {
        observeQuota()
        fetchInitialBookData()
        observeChatHistory()
    }

    private fun fetchInitialBookData() {
        viewModelScope.launch {
            val ready = bookRepository.isReady(bookId)
            val book = bookRepository.getBookDetails(bookId)

            _uiState.update {
                it.copy(
                    isReady = ready,
                    isIndexing = !ready,
                    bookTitle = book?.title ?: EMPTY,
                    bookAuthor = book?.author ?: EMPTY,
                    lastChapterTitle = book?.lastChapterTitle,
                    hasStartedReading = book?.lastChapterTitle != null,
                    spoilerLockEnabled = book?.spoilerLockEnabled ?: true
                )
            }
        }
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            airaChatRepository.getMessagesForBook(bookId).collectLatest { messages ->
                if (!isCurrentlyStreaming) {
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun ask(question: String) {
        if (question.isBlank() || _uiState.value.isLoading) return

        if (!_uiState.value.canSendQuery) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }

        val userMessage = AiraMessage(AiraMessage.Role.USER, question.trim())
        val initialAiraMessage = AiraMessage(AiraMessage.Role.AIRA, EMPTY)

        isCurrentlyStreaming = true
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + initialAiraMessage,
                isLoading = true,
                isStreaming = false,
                error = null
            )
        }

        generationJob = viewModelScope.launch {
            var hasConsumedQuota = false

            try {
                airaChatRepository.ask(bookId = bookId, question = question).collect { response ->

                    if (!hasConsumedQuota && response is AiraResponse.Success) {
                        hasConsumedQuota = true
                        currentUserId?.let { uid ->
                            viewModelScope.launch { quotaRepository.consumeQuery(uid) }
                        }
                    }

                    handleStreamResponse(response)
                }
            } finally {
                isCurrentlyStreaming = false
                _uiState.update { it.copy(isLoading = false, isStreaming = false) }
            }
        }
    }

    fun stopGeneration() {
        resetStreamingStates()
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            if (updatedMessages.isNotEmpty() && updatedMessages.last().role == AiraMessage.Role.AIRA) {
                updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(
                    content = "Stopped on your request. Let me know if you've got more questions!"
                )
            }
            state.copy(messages = updatedMessages)
        }
    }

    fun toggleSpoilerLock() {
        val newValue = !_uiState.value.spoilerLockEnabled

        _uiState.update { it.copy(spoilerLockEnabled = newValue) }
        viewModelScope.launch {
            bookRepository.setSpoilerLock(bookId, newValue)
        }
    }

    fun clearChat() {
        resetStreamingStates()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
        viewModelScope.launch {
            airaChatRepository.clearMessagesForBook(bookId)
        }
    }

    fun clearError() {
        _uiState.update { airaUiState ->
            airaUiState.copy(
                error = null
            )
        }
    }

    private fun observeQuota() {
        viewModelScope.launch {
            quotaRepository.canSendQuery.collectLatest { canSend ->
                _uiState.update { airaUiState ->
                    airaUiState.copy(
                        canSendQuery = canSend
                    )
                }
            }
        }
        viewModelScope.launch {
            quotaRepository.remainingQueries.collectLatest { remaining ->
                _uiState.update { airaUiState ->
                    airaUiState.copy(
                        remainingQueries = remaining
                    )
                }
            }
        }
    }

    private fun handleStreamResponse(response: AiraResponse) {
        when (response) {
            is AiraResponse.Success -> {
                _uiState.update { airaUiState ->
                    val updatedMessages = airaUiState.messages.toMutableList()
                    if (updatedMessages.isNotEmpty() && updatedMessages.last().role == AiraMessage.Role.AIRA) {
                        updatedMessages[updatedMessages.lastIndex] =
                            updatedMessages.last().copy(content = response.text)
                    }
                    airaUiState.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        isStreaming = true
                    )
                }
            }

            is AiraResponse.Error -> {
                _uiState.update { airaUiState ->
                    airaUiState.copy(
                        messages = airaUiState.messages.dropLast(1),
                        error = response.message
                    )
                }
            }
        }
    }

    private fun resetStreamingStates() {
        generationJob?.cancel()
        generationJob = null
        isCurrentlyStreaming = false
        _uiState.update { airaUiState ->
            airaUiState.copy(
                isLoading = false,
                isStreaming = false
            )
        }
    }
}