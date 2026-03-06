package com.yugentech.quill.aira.aira.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.aira.AiraMessage
import com.yugentech.quill.aira.aira.repository.AiraRepository
import com.yugentech.quill.aira.aira.AiraResponse
import com.yugentech.quill.aira.aira.AiraUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiraViewModel(
    private val airaRepository: AiraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiraUiState())
    val uiState = _uiState.asStateFlow()

    private var currentBookId: String? = null

    private var isCurrentlyStreaming = false

    fun initForBook(bookId: String) {
        if (currentBookId == bookId) return
        currentBookId = bookId

        _uiState.update { AiraUiState() }

        viewModelScope.launch {
            val ready = airaRepository.isReady(bookId)
            val spoilerLock = airaRepository.isSpoilerLockEnabled(bookId)
            _uiState.update {
                it.copy(
                    isReady = ready,
                    isIndexing = !ready,
                    spoilerLockEnabled = spoilerLock
                )
            }
        }

        viewModelScope.launch {
            airaRepository.getMessagesForBook(bookId).collectLatest { messages ->
                if (!isCurrentlyStreaming) {
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    fun toggleSpoilerLock() {
        val bookId = currentBookId ?: return
        val newValue = !_uiState.value.spoilerLockEnabled
        _uiState.update { it.copy(spoilerLockEnabled = newValue) }
        viewModelScope.launch {
            airaRepository.setSpoilerLock(bookId, newValue)
        }
    }

    fun ask(question: String) {
        val bookId = currentBookId ?: return
        if (question.isBlank()) return
        if (_uiState.value.isLoading) return

        val userMessage = AiraMessage(AiraMessage.Role.USER, question.trim())
        val initialAiraMessage = AiraMessage(AiraMessage.Role.AIRA, "")

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + initialAiraMessage,
                isLoading = true,
                isStreaming = false,
                error = null
            )
        }

        isCurrentlyStreaming = true

        viewModelScope.launch {
            airaRepository.ask(bookId = bookId, question = question).collect { response ->
                when (response) {
                    is AiraResponse.Success -> {
                        _uiState.update { state ->
                            val updatedMessages = state.messages.toMutableList()

                            if (updatedMessages.isNotEmpty() && updatedMessages.last().role == AiraMessage.Role.AIRA) {
                                updatedMessages[updatedMessages.lastIndex] =
                                    updatedMessages.last().copy(content = response.text)
                            }

                            state.copy(
                                messages = updatedMessages,
                                isLoading = false,
                                isStreaming = true
                            )
                        }
                    }
                    is AiraResponse.Error -> {
                        isCurrentlyStreaming = false
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.dropLast(1),
                                isLoading = false,
                                isStreaming = false,
                                error = response.message
                            )
                        }
                    }
                    is AiraResponse.IndexingNotReady -> {
                        isCurrentlyStreaming = false
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.dropLast(1),
                                isLoading = false,
                                isStreaming = false,
                                isIndexing = true,
                                isReady = false,
                                error = "Aira is still indexing this book. Please wait a moment."
                            )
                        }
                    }
                    is AiraResponse.NoChaptersRead -> {
                        isCurrentlyStreaming = false
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.dropLast(1),
                                isLoading = false,
                                isStreaming = false,
                                error = "Start reading to unlock Aira's knowledge of this book."
                            )
                        }
                    }
                }
            }

            isCurrentlyStreaming = false
            _uiState.update { it.copy(isLoading = false, isStreaming = false) }

            launch {
                airaRepository.getMessagesForBook(bookId).collectLatest { finalMessages ->
                    if (!isCurrentlyStreaming) {
                        _uiState.update { it.copy(messages = finalMessages) }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun recheckReadiness() {
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            val ready = airaRepository.isReady(bookId)
            _uiState.update { it.copy(isReady = ready, isIndexing = !ready) }
        }
    }
}