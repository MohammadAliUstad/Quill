package com.yugentech.quill.bookDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.bookDetails.state.BookDetailsUiState
import com.yugentech.quill.database.converter.AppJson
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.domain.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class BookDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    billingRepository: BillingRepository,
    private val repository: BookDetailsRepository
) : ViewModel() {

    private val bookIdParam: String? = savedStateHandle["bookId"]
    private val bookJsonParam: String? = savedStateHandle["bookJson"]

    private val passedBook: Book? by lazy {
        bookJsonParam?.let {
            val decodedJson = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            AppJson.decodeFromString(decodedJson)
        }
    }

    private val targetBookId: String = passedBook?.id
        ?: bookIdParam
        ?: throw IllegalArgumentException("BookDetailsViewModel requires either bookId or bookJson")

    private val userExpandedOverride = MutableStateFlow<Boolean?>(null)

    val uiState: StateFlow<BookDetailsUiState> = combine(
        repository.getBook(targetBookId),
        userExpandedOverride,
        billingRepository.isPro
    ) { dbEntity, userExpanded, isPro ->

        val richBook = dbEntity?.toDomainModel() ?: passedBook
        if (richBook == null) return@combine BookDetailsUiState(isLoading = true)

        val isDownloaded = richBook.downloadStatus == DownloadStatus.DOWNLOADED
        val shouldExpand = userExpanded ?: !isDownloaded

        BookDetailsUiState(
            book = richBook,
            chapters = richBook.chapters,
            isLoading = false,
            isDescriptionExpanded = shouldExpand,
            isPro = isPro
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookDetailsUiState(
            book = passedBook,
            chapters = passedBook?.chapters ?: emptyList(),
            isLoading = true,
            isDescriptionExpanded = passedBook?.downloadStatus != DownloadStatus.DOWNLOADED
        )
    )

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onToggleDescription() {
        userExpandedOverride.value = !uiState.value.isDescriptionExpanded
    }

    fun onDownloadClick() {
        viewModelScope.launch {
            val currentState = uiState.value
            currentState.book?.let { book ->
                repository.startDownload(book, currentState.isPro)
            }
        }
    }

    fun deleteBook() {
        userExpandedOverride.value = true
        viewModelScope.launch {
            repository.removeDownload(targetBookId)
            repository.removeFromRecent(targetBookId)
        }
    }

    fun onCategoryChange(newCategory: String) {
        viewModelScope.launch {
            uiState.value.book?.let { repository.updateCategory(it, newCategory) }
        }
    }

    fun removeFromLibrary() {
        viewModelScope.launch {
            repository.deleteBook(targetBookId)
        }
    }

    fun onFavoriteToggle() {
        viewModelScope.launch {
            uiState.value.book?.let { repository.updateFavorite(it, !it.isFavorite) }
        }
    }

    fun resetReadingProgress() {
        viewModelScope.launch {
            repository.resetReadingProgress(targetBookId)
        }
    }
}