package com.yugentech.quill.bookDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.bookDetails.state.BookDetailsUiState
import com.yugentech.quill.database.converter.AppJson
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class BookDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BookDetailsRepository,
    private val maintenanceMonitor: SourceMaintenanceMonitor
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

    /** The catalogue copy shown in place of a book just taken out of the library. */
    private val fetchedBook = MutableStateFlow<Book?>(null)

    init {
        // A book not downloaded yet can only be fetched while its source is up: find out now, so
        // the download button can say so instead of failing. Library books open by id, so the
        // source comes from the saved copy.
        viewModelScope.launch {
            val source = passedBook?.source ?: repository.getBook(targetBookId).first()?.toDomainModel()?.source
            if (source != null) maintenanceMonitor.checkIfDue(source)
        }
    }

    private val bookState = combine(
        repository.getBook(targetBookId),
        userExpandedOverride,
        fetchedBook
    ) { dbEntity, userExpanded, fetched ->

        val richBook = dbEntity?.toDomainModel() ?: fetched ?: passedBook
        if (richBook == null) return@combine BookDetailsUiState(isLoading = true)

        val isDownloaded = richBook.downloadStatus == DownloadStatus.DOWNLOADED
        val shouldExpand = userExpanded ?: !isDownloaded

        BookDetailsUiState(
            book = richBook,
            chapters = richBook.chapters,
            isLoading = false,
            isDescriptionExpanded = shouldExpand
        )
    }

    val uiState: StateFlow<BookDetailsUiState> = combine(bookState, maintenanceMonitor.down) { state, down ->
        val maintenance = state.book?.let { down[it.source] }
        state.copy(isSourceDown = maintenance != null, sourceBackAfter = maintenance?.backAfter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookDetailsUiState(
            book = fetchedBook.value ?: passedBook,
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
            uiState.value.book?.let { repository.startDownload(it) }
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

    /**
     * Takes the book out of the library: its download, progress, shelf and favourite go. A book
     * from a catalogue stays on screen as it would be found there, fresh, to add again if
     * wanted; an imported book has nothing to come back to, so the screen closes on it.
     */
    fun removeFromLibrary() {
        uiState.value.book?.takeIf { it.source != BookSource.USER_IMPORTED }?.let { book ->
            // Shown once the saved copy is gone, in its place
            fetchedBook.value = Book(
                id = book.id,
                title = book.title,
                author = book.author,
                description = book.description,
                coverUrl = book.coverUrl,
                downloadUrl = book.downloadUrl,
                source = book.source,
                subjects = book.subjects,
                language = book.language
            )
            userExpandedOverride.value = true
        }
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