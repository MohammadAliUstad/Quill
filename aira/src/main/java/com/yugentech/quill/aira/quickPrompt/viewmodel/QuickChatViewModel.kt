package com.yugentech.quill.aira.quickPrompt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.quickPrompt.repository.QuickPromptRepository
import com.yugentech.quill.aira.quickPrompt.state.QuickPrompt
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuickChatViewModel(
    private val quickPromptRepository: QuickPromptRepository,
    private val quotaRepository: QuotaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickChatUiState())
    val uiState = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var currentBookId: String? = null
    private val currentUserId: String? get() = authRepository.currentUser


    fun handle(intent: QuickPrompt) {
        val bookId = currentBookId ?: return
        if (_uiState.value.isLoading) return

        activeJob?.cancel()

        activeJob = viewModelScope.launch {
            val canSend = quotaRepository.canSendQuery.first()
            if (!canSend) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }

            _uiState.update { QuickChatUiState(isLoading = true) }
            var hasConsumedQuota = false

            quickPromptRepository.handle(bookId, intent).collect { response ->
                when (response) {
                    is AiraResponse.Success -> {
                        if (!hasConsumedQuota) {
                            hasConsumedQuota = true
                            currentUserId?.let { uid ->
                                launch { quotaRepository.consumeQuery(uid) }
                            }
                        }

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
                            QuickChatUiState(
                                isLoading = false,
                                isStreaming = false,
                                error = response.message
                            )
                        }
                    }
                }
            }

            _uiState.update { it.copy(isStreaming = false) }
        }
    }
}