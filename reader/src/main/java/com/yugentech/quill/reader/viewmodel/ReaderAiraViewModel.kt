package com.yugentech.quill.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.aira.quickPrompt.repository.QuickPromptRepository
import com.yugentech.quill.aira.quickPrompt.state.QuickPrompt
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderAiraViewModel(
    private val airaChatRepository: AiraChatRepository,
    private val quickPromptRepository: QuickPromptRepository,
    private val quotaRepository: QuotaRepository,
    private val authRepository: AuthRepository,
    private val bookRepository: BookRepository // NEW: Injected BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderAiraUiState())
    val uiState = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private val currentUserId: String? get() = authRepository.currentUser

    init {
        // Automatically keep track of quota status
        viewModelScope.launch {
            quotaRepository.canSendQuery.collectLatest { canSend ->
                _uiState.update { it.copy(canSendQuery = canSend) }
            }
        }
    }

    // --- INDEXING CHECK ---
    fun checkIndexingStatus(bookId: String) {
        viewModelScope.launch {
            val ready = bookRepository.isReady(bookId)
            _uiState.update { it.copy(isIndexed = ready) }
        }
    }

    // --- 1. MANUAL CHAT (Saves to DB via Repository) ---
    fun ask(bookId: String, question: String) {
        if (question.isBlank() || _uiState.value.isLoading) return
        if (!checkQuota()) return

        prepareForNewQuery()

        activeJob = viewModelScope.launch {
            var hasConsumedQuota = false
            try {
                airaChatRepository.ask(bookId, question).collect { response ->
                    handleResponseStream(response) {
                        if (!hasConsumedQuota) {
                            hasConsumedQuota = true
                            consumeQuota()
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false, isStreaming = false) }
            }
        }
    }

    // --- 2. QUICK PROMPTS (Chips - Does NOT save to DB) ---
    fun handleQuickPrompt(bookId: String, intent: QuickPrompt) {
        if (_uiState.value.isLoading) return
        if (!checkQuota()) return

        prepareForNewQuery()

        activeJob = viewModelScope.launch {
            var hasConsumedQuota = false
            try {
                quickPromptRepository.handle(bookId, intent).collect { response ->
                    handleResponseStream(response) {
                        if (!hasConsumedQuota) {
                            hasConsumedQuota = true
                            consumeQuota()
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false, isStreaming = false) }
            }
        }
    }

    // --- SHARED STREAM HANDLER ---
    private fun handleResponseStream(response: AiraResponse, onFirstSuccess: () -> Unit) {
        when (response) {
            is AiraResponse.Success -> {
                onFirstSuccess() // Deduct quota only once per successful stream
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = true,
                        response = response.text,
                        error = null
                    )
                }
            }
            is AiraResponse.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        error = response.message
                    )
                }
            }
        }
    }

    private fun prepareForNewQuery() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(isLoading = true, isStreaming = false, response = "", error = null)
        }
    }

    private fun checkQuota(): Boolean {
        if (!_uiState.value.canSendQuery) {
            _uiState.update { it.copy(showPaywall = true) }
            return false
        }
        return true
    }

    private fun consumeQuota() {
        currentUserId?.let { uid ->
            viewModelScope.launch { quotaRepository.consumeQuery(uid) }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update { state ->
            val stoppedMsg = if (state.response.isNullOrBlank()) {
                "Stopped on your request."
            } else state.response

            state.copy(
                isLoading = false,
                isStreaming = false,
                response = stoppedMsg
            )
        }
    }

    fun clearResponse() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update {
            it.copy(isLoading = false, isStreaming = false, response = null, error = null)
        }
    }

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }
}