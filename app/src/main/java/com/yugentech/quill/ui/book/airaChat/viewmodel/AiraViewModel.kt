package com.yugentech.quill.ui.book.airaChat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.aira.repository.AiraChatRepository
import com.yugentech.quill.ui.book.airaChat.state.AiraUiState
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.util.AiraLog
import kotlinx.coroutines.CancellationException
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.theme.tokens.AppConstants.EMPTY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.yugentech.quill.R
import com.yugentech.quill.localization.AppStrings

class AiraViewModel(
    val bookId: String,
    private val airaChatRepository: AiraChatRepository,
    private val bookRepository: BookDetailsRepository,
    private val quotaRepository: QuotaRepository,
    private val authRepository: AuthRepository,
    private val workManager: WorkManager,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiraUiState())
    val uiState = _uiState.asStateFlow()

    private var isCurrentlyStreaming = false
    private var generationJob: Job? = null

    private val currentUserId: String? get() = authRepository.currentUser

    init {
        observeQuota()
        observeBilling()
        observeBookAndIndexing()
        observeChatHistory()
    }

    private fun observeBilling() {
        viewModelScope.launch {
            billingRepository.isPro.collectLatest { proStatus ->
                _uiState.update { it.copy(isPro = proStatus) }
            }
        }
    }

    private fun observeBookAndIndexing() {
        // Followed, not read once: reading a few pages with this screen in the back stack opens
        // it up on the way back
        viewModelScope.launch {
            bookRepository.getBook(bookId).collect { book ->
                _uiState.update {
                    it.copy(
                        bookTitle = book?.title ?: EMPTY,
                        bookAuthor = book?.author ?: EMPTY,
                        lastChapterTitle = book?.lastChapterTitle,
                        hasStartedReading = book?.lastChapterTitle != null,
                        spoilerLockEnabled = book?.spoilerLockEnabled ?: true,
                        isBookLoaded = true
                    )
                }
            }
        }

        // Ready only once the book's whole index is saved, which the worker marks as its very
        // last step. The work's own states can't say that: a guest's work "succeeds" without
        // indexing anything, and a book's chunks exist long before its index is finished.
        val isComplete = bookRepository.observeIsReady(bookId)
        val work = workManager.getWorkInfosByTagLiveData(BookEmbeddingWorker.tagFor(bookId)).asFlow()

        combine(isComplete, work) { complete, workInfos ->
            // Running beats waiting: a book can have more than one request for it at a time.
            // Waiting is ENQUEUED (about to start, or retrying) or BLOCKED (queued behind other
            // books in the indexing queue).
            val running = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            val waiting = workInfos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
            }
            val failed = workInfos.lastOrNull()?.state == WorkInfo.State.FAILED
            // A finished index wins over any request still around for the book: a fresh download
            // clears it before indexing again, and a request for an indexed book just ends at once
            IndexStatus(
                ready = complete,
                running = !complete && running != null,
                queued = !complete && running == null && waiting,
                progress = running?.progress?.getInt(BookEmbeddingWorker.KEY_PROGRESS, 0) ?: 0,
                failed = !complete && running == null && !waiting && failed
            )
        }
            .distinctUntilChanged()
            .onEach { status ->
                _uiState.update {
                    it.copy(
                        isReady = status.ready,
                        // Not ready and nothing to wait on (not asked for yet) shows as reading
                        // too, as it always has: it's about to be
                        isIndexing = status.running || (!status.ready && !status.queued && !status.failed),
                        isQueued = status.queued,
                        indexingProgress = if (status.ready) 100 else status.progress,
                        error = if (status.failed) AppStrings.get(R.string.indexing_failed) else it.error,
                        isIndexStatusLoaded = true
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class IndexStatus(
        val ready: Boolean,
        val running: Boolean,
        val queued: Boolean,
        val progress: Int,
        val failed: Boolean
    )

    private fun observeChatHistory() {
        viewModelScope.launch {
            airaChatRepository.getMessagesForBook(bookId).collectLatest { messages ->
                if (!isCurrentlyStreaming) {
                    _uiState.update { it.copy(messages = messages, areMessagesLoaded = true) }
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
                airaChatRepository.ask(bookId = bookId, query = question).collect { response ->

                    if (!hasConsumedQuota && response is AiraResponse.Success) {
                        hasConsumedQuota = true
                        currentUserId?.let { uid ->
                            viewModelScope.launch { quotaRepository.consumeQuery(uid) }
                        }
                    }

                    handleStreamResponse(response)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) AiraLog.failure("ChatScreen ask", e)
                throw e
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
                    content = AppStrings.get(R.string.aira_stopped)
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
                        updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(
                            content = response.text
                        )
                    }

                    airaUiState.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        isStreaming = true
                    )
                }
            }

            is AiraResponse.Error -> {
                AiraLog.w("Chat showed an error")
                _uiState.update { airaUiState ->
                    airaUiState.copy(
                        messages = airaUiState.messages.dropLast(1),
                        error = response.message
                    )
                }
            }

            // General/book chat never produces an image -- that's exclusive to the
            // quick-action "Visualize this" flow, which doesn't go through this ViewModel.
            is AiraResponse.ImageSuccess -> Unit
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
