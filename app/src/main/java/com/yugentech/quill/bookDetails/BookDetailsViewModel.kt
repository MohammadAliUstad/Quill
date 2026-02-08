package com.yugentech.quill.bookDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.network.AppJson
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.room.entities.Chapter
import com.yugentech.quill.room.entities.DownloadStatus
import com.yugentech.quill.room.entities.LibraryBookEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class BookDetailsUiState(
    val book: Book,
    val chapters: List<Chapter> = emptyList(),
    val libraryBook: LibraryBookEntity? = null,
    val isLoading: Boolean = true,
    val isDescriptionExpanded: Boolean = true // Default value (will be overwritten instantly on load)
)

class BookDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BookDetailsRepository
) : ViewModel() {

    private val bookJson: String = checkNotNull(savedStateHandle["book_json"])
    private val passedBook: Book by lazy {
        val decodedJson = URLDecoder.decode(bookJson, StandardCharsets.UTF_8.toString())
        AppJson.decodeFromString(decodedJson)
    }

    private val _uiState = MutableStateFlow(BookDetailsUiState(book = passedBook))
    val uiState = _uiState.asStateFlow()

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 1. SNAPSHOT: Get initial data
            val detailSnapshot = repository.getDetailsSnapshot(passedBook.id)
            val libSnapshot = repository.getLibraryBookSnapshot(passedBook.id)

            // LOGIC:
            // - If Downloaded -> Start CLOSED
            // - If Not Downloaded -> Start OPEN
            val isDownloaded = libSnapshot?.downloadStatus == DownloadStatus.DOWNLOADED
            val initialExpandedState = !isDownloaded

            _uiState.update { state ->
                state.copy(
                    book = passedBook.copy(
                        description = detailSnapshot?.description ?: passedBook.description,
                        subjects = detailSnapshot?.subjects ?: passedBook.subjects
                    ),
                    chapters = detailSnapshot?.chapters ?: emptyList(),
                    libraryBook = libSnapshot,
                    isLoading = false,
                    isDescriptionExpanded = initialExpandedState // Apply calculated state
                )
            }

            // 2. STREAM: Watch for updates (e.g., Download Finishing)
            combine(
                repository.getDetails(passedBook.id),
                repository.getLibraryBook(passedBook.id)
            ) { details, libraryBook ->

                _uiState.update { currentState ->
                    // Check if we just transitioned from DOWNLOADING -> DOWNLOADED
                    val wasDownloading = currentState.libraryBook?.downloadStatus == DownloadStatus.DOWNLOADING
                    val isNowDownloaded = libraryBook?.downloadStatus == DownloadStatus.DOWNLOADED

                    // If we just watched it finish downloading, auto-close the description
                    val shouldCollapse = wasDownloading && isNowDownloaded

                    currentState.copy(
                        book = passedBook.copy(
                            description = details?.description ?: passedBook.description,
                            subjects = details?.subjects ?: passedBook.subjects
                        ),
                        chapters = details?.chapters ?: emptyList(),
                        libraryBook = libraryBook,
                        // Preserve user's toggle state unless we force collapse
                        isDescriptionExpanded = if (shouldCollapse) false else currentState.isDescriptionExpanded
                    )
                }
            }.collect()
        }
    }

    // --- Actions ---

    fun onToggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun onDownloadClick() {
        viewModelScope.launch {
            repository.startDownload(passedBook)
        }
    }

    fun onRemoveDownloadClick() {
        viewModelScope.launch {
            repository.removeDownload(passedBook.id)
        }
    }

    fun onCategoryChange(newCategory: String) {
        viewModelScope.launch {
            val currentLibraryBook = _uiState.value.libraryBook
            if (currentLibraryBook != null) {
                repository.updateCategory(passedBook.id, newCategory)
                _uiState.update { currentState ->
                    currentState.copy(
                        libraryBook = currentLibraryBook.copy(userCategory = newCategory)
                    )
                }
            }
        }
    }

    fun onFavoriteToggle() {
        viewModelScope.launch {
            val currentLibraryBook = _uiState.value.libraryBook
            if (currentLibraryBook != null) {
                val newFavoriteStatus = !currentLibraryBook.isFavorite
                repository.toggleFavorite(passedBook.id, newFavoriteStatus)
                _uiState.update { currentState ->
                    currentState.copy(
                        libraryBook = currentLibraryBook.copy(isFavorite = newFavoriteStatus)
                    )
                }
            }
        }
    }
}