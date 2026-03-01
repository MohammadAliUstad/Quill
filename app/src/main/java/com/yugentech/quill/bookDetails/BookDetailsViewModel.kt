package com.yugentech.quill.bookDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.network.AppJson
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.network.domain.BookSource
import com.yugentech.quill.room.BookMappers.toDomainModel
import com.yugentech.quill.room.entities.Chapter
import com.yugentech.quill.room.entities.DownloadStatus
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
    val book: Book,
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

    private val initialLoadingState = passedBook == null || passedBook?.description.isNullOrBlank()

    private val _uiState = MutableStateFlow(
        BookDetailsUiState(
            book = passedBook ?: createPlaceholderBook(targetBookId),
            chapters = passedBook?.chapters ?: emptyList(),
            isLoading = initialLoadingState,
            isDescriptionExpanded = passedBook?.downloadStatus != DownloadStatus.DOWNLOADED
        )
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
                val richBook = dbEntity?.toDomainModel() ?: (
                        passedBook?.copy(
                            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                            isFavorite = false,
                            userCategory = null,
                            progressPercent = 0f,
                            localFilePath = null
                        ) ?: createPlaceholderBook(targetBookId)
                        )

                _uiState.update { currentState ->
                    val wasDownloading =
                        currentState.book.downloadStatus == DownloadStatus.DOWNLOADING
                    val isNowDownloaded = richBook.downloadStatus == DownloadStatus.DOWNLOADED

                    val isFirstLoad = currentState.isLoading

                    val nextExpandedState = when {
                        wasDownloading && isNowDownloaded -> false
                        isFirstLoad -> !isNowDownloaded
                        else -> currentState.isDescriptionExpanded
                    }

                    currentState.copy(
                        book = richBook,
                        chapters = richBook.chapters,
                        isLoading = false,
                        isDescriptionExpanded = nextExpandedState
                    )
                }
            }
        }
    }

    private fun createPlaceholderBook(id: String): Book {
        return Book(
            id = id,
            title = "",
            author = "",
            description = null,
            coverUrl = null,
            downloadUrl = "",
            source = BookSource.STANDARD_EBOOKS,
            subjects = emptyList(),
            language = ""
        )
    }

    fun onToggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun onDownloadClick() {
        viewModelScope.launch {
            repository.startDownload(_uiState.value.book)
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
            repository.updateCategory(_uiState.value.book, newCategory)
        }
    }

    fun removeFromLibrary() {
        viewModelScope.launch {
            val currentBook = _uiState.value.book
            repository.deleteBook(currentBook.id)
        }
    }

    fun onFavoriteToggle() {
        viewModelScope.launch {
            val currentBook = _uiState.value.book
            repository.updateFavorite(currentBook, !currentBook.isFavorite)
        }
    }

    fun resetReadingProgress() {
        viewModelScope.launch {
            repository.resetReadingProgress(targetBookId)
        }
    }
}