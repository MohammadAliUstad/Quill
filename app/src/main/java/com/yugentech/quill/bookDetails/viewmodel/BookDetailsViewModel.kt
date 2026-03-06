package com.yugentech.quill.bookDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.converter.AppJson
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(
        if (passedBook != null) {
            // Full book data available immediately — only show loading if description is missing
            BookDetailsUiState(
                book = passedBook,
                chapters = passedBook!!.chapters,
                isLoading = passedBook!!.description.isNullOrBlank(),
                isDescriptionExpanded = passedBook!!.downloadStatus != DownloadStatus.DOWNLOADED
            )
        } else {
            // Navigated by bookId only (library / allBooks) — Room will emit immediately
            BookDetailsUiState(
                book = null,
                chapters = emptyList(),
                isLoading = true,
                isDescriptionExpanded = true
            )
        }
    )
    val uiState = _uiState.asStateFlow()

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getBook(targetBookId).collectLatest { dbEntity ->
                val richBook = dbEntity?.toDomainModel()
                    ?: passedBook?.copy(
                        downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                        isFavorite = false,
                        userCategory = null,
                        progressPercent = 0f,
                        localFilePath = null
                    )
                    ?: return@collectLatest // Room returned null and no passedBook — nothing to show

                _uiState.update { current ->
                    val wasDownloading = current.book?.downloadStatus == DownloadStatus.DOWNLOADING
                    val isNowDownloaded = richBook.downloadStatus == DownloadStatus.DOWNLOADED

                    val nextExpandedState = when {
                        wasDownloading && isNowDownloaded -> false
                        current.isLoading -> !isNowDownloaded
                        else -> current.isDescriptionExpanded
                    }

                    current.copy(
                        book = richBook,
                        chapters = richBook.chapters.ifEmpty { current.chapters },
                        isLoading = false,
                        isDescriptionExpanded = nextExpandedState
                    )
                }
            }
        }
    }

    fun onToggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun onDownloadClick() {
        viewModelScope.launch {
            _uiState.value.book?.let { repository.startDownload(it) }
        }
    }

    fun deleteBook() {
        _uiState.update { it.copy(isDescriptionExpanded = true) }
        viewModelScope.launch {
            repository.removeDownload(targetBookId)
        }
    }

    fun onCategoryChange(newCategory: String) {
        viewModelScope.launch {
            _uiState.value.book?.let { repository.updateCategory(it, newCategory) }
        }
    }

    fun removeFromLibrary() {
        viewModelScope.launch {
            repository.deleteBook(targetBookId)
        }
    }

    fun onFavoriteToggle() {
        viewModelScope.launch {
            _uiState.value.book?.let { repository.updateFavorite(it, !it.isFavorite) }
        }
    }

    fun resetReadingProgress() {
        viewModelScope.launch {
            repository.resetReadingProgress(targetBookId)
        }
    }
}