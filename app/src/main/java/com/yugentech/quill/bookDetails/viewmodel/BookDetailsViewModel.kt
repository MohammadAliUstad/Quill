package com.yugentech.quill.bookDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.converter.AppJson
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class BookDetailsUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val isDescriptionExpanded: Boolean = true
)

class BookDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BookDetailsRepository
) : ViewModel() {

    // 1. Extract Navigation Arguments
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

    // 2. Track Manual User Interactions
    // Null means "let the database dictate if the description is open or closed"
    // True/False means "the user clicked the expand/collapse button, respect their choice"
    private val userExpandedOverride = MutableStateFlow<Boolean?>(null)

    // 3. The Pure Reactive State Flow
    val uiState: StateFlow<BookDetailsUiState> = combine(
        repository.getBook(targetBookId),
        userExpandedOverride
    ) { dbEntity, userExpanded ->

        // Prefer the fresh DB data. If it doesn't exist yet, use the JSON fallback to prevent a blank screen.
        val richBook = dbEntity?.toDomainModel() ?: passedBook

        if (richBook == null) {
            return@combine BookDetailsUiState(isLoading = true)
        }

        val isDownloaded = richBook.downloadStatus == DownloadStatus.DOWNLOADED

        // If the user manually toggled the description, use their choice.
        // Otherwise, automatically collapse it if the book is downloaded, and expand if it is not.
        val shouldExpand = userExpanded ?: !isDownloaded

        BookDetailsUiState(
            book = richBook,
            chapters = richBook.chapters,
            isLoading = false,
            isDescriptionExpanded = shouldExpand
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookDetailsUiState(
            book = passedBook,
            chapters = passedBook?.chapters ?: emptyList(),

            // FIX 1: Change this from (passedBook == null) to strictly true!
            // This hides the description section for 5 milliseconds until the DB
            // confirms the absolute true download status, eliminating the flicker.
            isLoading = true,

            isDescriptionExpanded = passedBook?.downloadStatus != DownloadStatus.DOWNLOADED
        )
    )

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ACTIONS ---

    fun onToggleDescription() {
        // Record the user's manual override
        userExpandedOverride.value = !uiState.value.isDescriptionExpanded
    }

    fun onDownloadClick() {
        viewModelScope.launch {
            uiState.value.book?.let { repository.startDownload(it) }
        }
    }

    fun deleteBook() {
        // Auto-expand description when file is removed
        userExpandedOverride.value = true
        viewModelScope.launch {
            repository.removeDownload(targetBookId)
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