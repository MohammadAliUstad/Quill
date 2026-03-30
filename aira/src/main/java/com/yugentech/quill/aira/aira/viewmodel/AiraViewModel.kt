package com.yugentech.quill.aira.aira.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.aira.book.IndexingRepository
import com.yugentech.quill.aira.rag.BookIndexingWorker
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
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
    private val authRepository: AuthRepository,
    private val indexingRepository: IndexingRepository
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
        observeIndexing()
    }

    private fun fetchInitialBookData() {
        viewModelScope.launch {
            val ready = indexingRepository.isBookReady(bookId)
            val book = bookRepository.getBookDetails(bookId)

            _uiState.update {
                it.copy(
                    isReady = ready,
                    isIndexing = !ready,
                    bookTitle = book?.title ?: "",
                    bookAuthor = book?.author ?: "",
                    lastChapterTitle = book?.lastChapterTitle,
                    hasStartedReading = book?.lastChapterTitle != null,
                    spoilerLockEnabled = book?.spoilerLockEnabled ?: true
                )
            }
        }
    }

    private fun observeIndexing() {
        viewModelScope.launch {
            indexingRepository.observeIndexing(bookId).collectLatest { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                        val progress = workInfo.progress.getInt(BookIndexingWorker.KEY_PROGRESS, 0)
                        val phase = workInfo.progress.getString(BookIndexingWorker.KEY_PHASE)
                        val isReady = indexingRepository.isBookReady(bookId)

                        _uiState.update {
                            it.copy(
                                isReady = isReady,
                                isIndexing = !isReady,
                                isIndexingInBackground = isReady,
                                indexingProgress = progress,
                                indexingPhase = phase
                            )
                        }
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        _uiState.update {
                            it.copy(
                                isReady = true,
                                isIndexing = false,
                                isIndexingInBackground = false,
                                indexingProgress = 100,
                                indexingPhase = null
                            )
                        }
                    }

                    WorkInfo.State.FAILED -> {
                        _uiState.update {
                            it.copy(
                                isIndexing = false,
                                isIndexingInBackground = false,
                                indexingPhase = null
                            )
                        }
                    }

                    WorkInfo.State.CANCELLED, null -> {
                        _uiState.update {
                            it.copy(
                                isIndexing = false,
                                isIndexingInBackground = false,
                                indexingPhase = null
                            )
                        }
                    }

                    else -> Unit
                }
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
        val initialAiraMessage = AiraMessage(AiraMessage.Role.AIRA, "")

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

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeQuota() {
        viewModelScope.launch {
            quotaRepository.canSendQuery.collectLatest { canSend ->
                _uiState.update { it.copy(canSendQuery = canSend) }
            }
        }
        viewModelScope.launch {
            quotaRepository.remainingQueries.collectLatest { remaining ->
                _uiState.update { it.copy(remainingQueries = remaining) }
            }
        }
    }

    private fun handleStreamResponse(response: AiraResponse) {
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
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.dropLast(1),
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
        _uiState.update { it.copy(isLoading = false, isStreaming = false) }
    }
}