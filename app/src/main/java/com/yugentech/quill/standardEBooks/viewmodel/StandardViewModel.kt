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

    // --- THE NEW SESSION CACHE ---
    // Maps a visited category/author/series to its fully loaded list and exact scroll page
    private val sessionCache = mutableMapOf<String, Pair<List<Book>, String?>>()

    init {
        viewModelScope.launch {
            standardRepository.getNewReleasesFlow().collect { cachedBooks ->
                if (_selectedCategory.value == "New Arrivals") {
                    _booksState.value = cachedBooks
                }
            }
        }

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
        nextPageUrl = null
        _error.value = null
        contentJob?.cancel()
        paginationJob?.cancel()

        when (category) {
            "New Arrivals" -> refreshNewReleases()
            "Collections" -> {
                _booksState.value = emptyList()
                fetchCollectionsList()
            }
            else -> loadCategory(category)
        }
    }

    private fun loadCategory(category: String) {
        // 1. Check Session Cache FIRST. If we've been here, restore instantly!
        sessionCache[category]?.let { (cachedBooks, nextUrl) ->
            _booksState.value = cachedBooks
            nextPageUrl = nextUrl
            _displayTitle.value = category
            return
        }

        // 2. Not in memory cache. Fallback to Hybrid DB + Network
        contentJob = viewModelScope.launch {
            _error.value = null

            val cached = standardRepository.getTopicBooksFlow(category).firstOrNull() ?: emptyList()
            _booksState.value = cached

            if (cached.isEmpty()) {
                _isLoading.value = true
                _displayTitle.value = "Browsing $category..."
            } else {
                _displayTitle.value = category
            }

            standardRepository.searchBooks("subject:\"$category\"")
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = if (result.books.isEmpty()) "No books found" else category

                    // SAVE TO SESSION CACHE for next time!
                    sessionCache[category] = Pair(result.books, result.nextPageUrl)
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
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
        val cacheKey = "Search: $query"
        _selectedCategory.value = cacheKey
        nextPageUrl = null

        sessionCache[cacheKey]?.let { (cachedBooks, nextUrl) ->
            _booksState.value = cachedBooks
            nextPageUrl = nextUrl
            _displayTitle.value = "Results for '$query'"
            return
        }

        _booksState.value = emptyList()
        performSearch(query = query, cacheKey = cacheKey)
    }

    fun onAuthorSelected(authorName: String) {
        val cacheKey = "Author: $authorName"
        _selectedCategory.value = cacheKey
        nextPageUrl = null
        contentJob?.cancel()

        sessionCache[cacheKey]?.let { (cachedBooks, nextUrl) ->
            _booksState.value = cachedBooks
            nextPageUrl = nextUrl
            _displayTitle.value = "Books by $authorName"
            return
        }

        _booksState.value = emptyList()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Finding books by $authorName..."
            _error.value = null

            standardRepository.getBooksByAuthor(authorName)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = if (result.books.isEmpty()) "No books found" else "Books by $authorName"
                    sessionCache[cacheKey] = Pair(result.books, result.nextPageUrl)
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    fun onCollectionSelected(collectionTitle: String, collectionUrl: String) {
        val cacheKey = "Series: $collectionTitle"
        _selectedCategory.value = cacheKey
        nextPageUrl = null
        contentJob?.cancel()

        sessionCache[cacheKey]?.let { (cachedBooks, nextUrl) ->
            _booksState.value = cachedBooks
            nextPageUrl = nextUrl
            _displayTitle.value = collectionTitle
            return
        }

        _booksState.value = emptyList()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Loading $collectionTitle..."
            _error.value = null

            standardRepository.getNextPage(collectionUrl)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = collectionTitle
                    sessionCache[cacheKey] = Pair(result.books, result.nextPageUrl)
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

            val cached = standardRepository.getNewReleasesFlow().firstOrNull() ?: emptyList()
            _booksState.value = cached

            if (cached.isEmpty()) {
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
        }
    }

    private fun performSearch(query: String, cacheKey: String) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _displayTitle.value = "Searching for '$query'..."

            standardRepository.searchBooks(query)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value = if (result.books.isEmpty()) "No books found for '$query'" else "Results for '$query'"
                    sessionCache[cacheKey] = Pair(result.books, result.nextPageUrl)
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
                    val updatedList = _booksState.value + result.books
                    _booksState.value = updatedList
                    nextPageUrl = result.nextPageUrl

                    // NEW: Keep the session cache updated with the new paginated items!
                    val currentCat = _selectedCategory.value
                    if (currentCat != "New Arrivals" && currentCat != "Collections") {
                        sessionCache[currentCat] = Pair(updatedList, result.nextPageUrl)
                    }
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