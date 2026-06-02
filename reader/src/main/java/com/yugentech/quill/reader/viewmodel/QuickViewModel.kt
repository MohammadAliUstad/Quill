package com.yugentech.quill.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.reader.repository.ReaderRepository
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.quick.repository.QuickRepository
import com.yugentech.quill.reader.state.QuickUiState
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.theme.tokens.AppConstants.EMPTY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuickViewModel(
    private val airaChatRepository: AiraChatRepository,
    private val quickRepository: QuickRepository,
    private val quotaRepository: QuotaRepository,
    private val authRepository: AuthRepository,
    private val readerRepository: ReaderRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickUiState())
    val uiState = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private val currentUserId: String? get() = authRepository.currentUser

    init {
        viewModelScope.launch {
            quotaRepository.canSendQuery.collectLatest { canSend ->
                _uiState.update { it.copy(canSendQuery = canSend) }
            }
        }
        viewModelScope.launch {
            billingRepository.isPro.collectLatest { isPro ->
                _uiState.update { it.copy(isPro = isPro) }
            }
        }
    }

    fun observeIndexingStatus(bookId: String) {
        viewModelScope.launch {
            readerRepository.observeIsReady(bookId).collectLatest { isIndexed ->
                _uiState.update { it.copy(isReady = isIndexed) }
            }
        }
    }

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

    fun handleQuickPrompt(bookId: String, intent: QuickPrompt) {
        if (_uiState.value.isLoading) return
        if (!checkQuota()) return

        prepareForNewQuery()

        activeJob = viewModelScope.launch {
            var hasConsumedQuota = false
            try {
                quickRepository.ask(bookId, intent).collect { response ->
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

    private fun handleResponseStream(response: AiraResponse, onFirstSuccess: () -> Unit) {
        when (response) {
            is AiraResponse.Success -> {
                onFirstSuccess()
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
            it.copy(
                isLoading = true,
                isStreaming = false,
                response = EMPTY,
                error = null
            )
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
            it.copy(
                isLoading = false,
                isStreaming = false,
                response = null,
                error = null
            )
        }
    }
}
