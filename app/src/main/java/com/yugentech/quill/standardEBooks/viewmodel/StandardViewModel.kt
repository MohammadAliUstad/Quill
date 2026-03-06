package com.yugentech.quill.standardEBooks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.standardEBooks.model.OpdsCollection
import com.yugentech.quill.standardEBooks.repository.StandardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class StandardNavigationEvent {
    data class NavigateById(val id: String) : StandardNavigationEvent()
    data class NavigateByContent(val book: Book) : StandardNavigationEvent()
}

private val BASE_CATEGORIES = listOf("New Arrivals", "Collections")

class StandardViewModel(
    private val standardRepository: StandardRepository,
    private val bookDetailsRepository: BookDetailsRepository
) : ViewModel() {

    // ── Content ───────────────────────────────────────────────────────────────
    private val _booksState = MutableStateFlow<List<Book>>(emptyList())
    val booksState = _booksState.asStateFlow()

    private val _collectionsState = MutableStateFlow<List<OpdsCollection>>(emptyList())
    val collectionsState = _collectionsState.asStateFlow()

    // ── UI Status ─────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private val _displayTitle = MutableStateFlow("")
    val displayTitle = _displayTitle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // ── Navigation ────────────────────────────────────────────────────────────
    private val _navigationEvent = MutableSharedFlow<StandardNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // ── Categories ────────────────────────────────────────────────────────────
    private val _selectedCategory = MutableStateFlow("New Arrivals")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow(BASE_CATEGORIES)
    val categories = _categories.asStateFlow()

    private var nextPageUrl: String? = null
    private var contentJob: Job? = null
    private var paginationJob: Job? = null

    init {
        // Stream cached books to UI whenever New Arrivals is active
        viewModelScope.launch {
            standardRepository.getNewReleasesFlow().collect { cachedBooks ->
                if (_selectedCategory.value == "New Arrivals") {
                    _booksState.value = cachedBooks
                }
            }
        }

        // Stream cached categories instantly, prepending base chips
        viewModelScope.launch {
            standardRepository.getCategoriesFlow().collect { cachedCategories ->
                _categories.value = BASE_CATEGORIES + cachedCategories
            }
        }

        refreshNewReleases()
        syncCategories()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onBookClick(networkBook: Book) {
        viewModelScope.launch {
            val isLocal = bookDetailsRepository.isBookInLibrary(networkBook.id)
            if (isLocal) {
                _navigationEvent.emit(StandardNavigationEvent.NavigateById(networkBook.id))
            } else {
                _navigationEvent.emit(StandardNavigationEvent.NavigateByContent(networkBook))
            }
        }
    }

    // ── Category & Discovery ──────────────────────────────────────────────────

    fun onCategorySelected(category: String) {
        if (_selectedCategory.value == category) return

        _selectedCategory.value = category
        _booksState.value = emptyList()
        nextPageUrl = null
        _error.value = null
        contentJob?.cancel()
        paginationJob?.cancel()

        when (category) {
            "New Arrivals" -> refreshNewReleases()
            "Collections" -> fetchCollectionsList()
            else -> performSearch(query = category, isCategorySearch = true)
        }
    }

    private fun fetchCollectionsList() {
        if (_collectionsState.value.isNotEmpty()) {
            _displayTitle.value = "Browse Series"
            return
        }

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Loading collections..."

            standardRepository.getCollections()
                .onSuccess { collections ->
                    _collectionsState.value = collections
                    _displayTitle.value = "Browse Series"
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    fun onSearchQuery(query: String) {
        if (query.isBlank()) return
        _selectedCategory.value = "Search"
        nextPageUrl = null
        performSearch(query = query, isCategorySearch = false)
    }

    fun onAuthorSelected(authorName: String) {
        _selectedCategory.value = "Author: $authorName"
        _booksState.value = emptyList()
        nextPageUrl = null
        contentJob?.cancel()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Finding books by $authorName..."
            _error.value = null

            standardRepository.getBooksByAuthor(authorName)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value =
                        if (result.books.isEmpty()) "No books found" else "Books by $authorName"
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    fun onCollectionSelected(collectionTitle: String, collectionUrl: String) {
        _selectedCategory.value = "Series: $collectionTitle"
        _booksState.value = emptyList()
        nextPageUrl = null
        contentJob?.cancel()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Loading $collectionTitle..."
            _error.value = null

            standardRepository.getNextPage(collectionUrl)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = collectionTitle
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    // ── Core Data Fetching ────────────────────────────────────────────────────

    private fun refreshNewReleases() {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            _error.value = null

            val cached = standardRepository.getNewReleasesFlow().firstOrNull()
            if (cached.isNullOrEmpty()) {
                _isLoading.value = true
            }
            _displayTitle.value = "Fetching new releases..."

            val result = standardRepository.syncNewReleases()
            _displayTitle.value = if (result.isSuccess) "Latest additions" else "Showing offline cache"
            _isLoading.value = false
        }
    }

    private fun syncCategories() {
        viewModelScope.launch {
            standardRepository.syncCategories()
            // Flow collector above handles UI update automatically
        }
    }

    private fun performSearch(query: String, isCategorySearch: Boolean) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _displayTitle.value =
                if (isCategorySearch) "Browsing $query..." else "Searching for '$query'..."

            standardRepository.searchBooks(query)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = when {
                        result.books.isEmpty() -> "No books found for '$query'"
                        isCategorySearch -> query
                        else -> "Results for '$query'"
                    }
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        val url = nextPageUrl
        if (url == null || _isPaginating.value || _isLoading.value) return

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            _isPaginating.value = true

            standardRepository.getNextPage(url)
                .onSuccess { result ->
                    _booksState.value += result.books
                    nextPageUrl = result.nextPageUrl
                }
                .onFailure {
                    // Keep existing list, just stop paginating
                }

            _isPaginating.value = false
        }
    }

    // ── Error Handling ────────────────────────────────────────────────────────

    private fun handleError(e: Throwable) {
        _error.value = e.localizedMessage
        _displayTitle.value = "Something went wrong"
    }
}