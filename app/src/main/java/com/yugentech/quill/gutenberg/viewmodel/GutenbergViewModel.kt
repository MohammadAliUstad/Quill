package com.yugentech.quill.gutenberg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.gutenberg.repository.GutenbergRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class GutenbergNavigationEvent {
    data class NavigateByContent(val book: Book) : GutenbergNavigationEvent()
}

class GutenbergViewModel(
    private val repository: GutenbergRepository
) : ViewModel() {

    // ── Content & UI Status ───────────────────────────────────────────────────
    private val _booksState = MutableStateFlow<List<Book>>(emptyList())
    val booksState = _booksState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private val _displayTitle = MutableStateFlow("")
    val displayTitle = _displayTitle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<GutenbergNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()

    // ── Internal State ────────────────────────────────────────────────────────
    private var nextPageUrl: String? = null
    private var contentJob: Job? = null
    private var paginationJob: Job? = null
    private var initialCategorySelected = false

    // CRITICAL FIX: Caches to prevent Flow-wipeouts and empty screens
    private var cachedPopularBooks = emptyList<Book>()
    private var paginatedMemoryCache = mutableListOf<Book>()

    init {
        viewModelScope.launch {
            repository.getPopularBooksFlow().collect { cachedBooks ->
                cachedPopularBooks = cachedBooks // Save for search clearing
                if (_selectedCategory.value.isBlank()) {
                    // Combine DB Page 1 + Memory Pages 2..N
                    _booksState.value = cachedBooks + paginatedMemoryCache
                }
            }
        }

        viewModelScope.launch {
            repository.getCategoriesFlow().collect { cachedCategories ->
                _categories.value = cachedCategories
                if (!initialCategorySelected && cachedCategories.isNotEmpty()) {
                    initialCategorySelected = true
                    onCategorySelected(cachedCategories.first())
                }
            }
        }

        syncPopularFeed()
    }

    fun onBookClick(book: Book) {
        viewModelScope.launch {
            _navigationEvent.emit(GutenbergNavigationEvent.NavigateByContent(book))
        }
    }

    fun onCategorySelected(category: String) {
        if (_selectedCategory.value == category) return

        _selectedCategory.value = category
        nextPageUrl = null
        _error.value = null
        paginatedMemoryCache.clear() // CRITICAL FIX: Clear old pagination

        contentJob?.cancel()
        paginationJob?.cancel()

        contentJob = viewModelScope.launch {
            val cached = repository.getTopicBooksFlow(category).firstOrNull()
            if (cached.isNullOrEmpty()) {
                _isLoading.value = true
                _booksState.value = emptyList()
            } else {
                _booksState.value = cached
            }

            _displayTitle.value = category
            repository.syncTopicBooks(category)

            repository.getTopicBooksFlow(category).collect { books ->
                if (_selectedCategory.value == category) {
                    // Combine DB Page 1 + Memory Pages 2..N
                    _booksState.value = books + paginatedMemoryCache
                    _isLoading.value = false
                }
            }
        }
    }

    fun onSearchQuery(query: String) {
        if (query.isBlank()) {
            paginatedMemoryCache.clear()
            _selectedCategory.value = ""
            _displayTitle.value = ""
            // CRITICAL FIX: Restore popular books immediately (Flow won't re-emit if DB didn't change)
            _booksState.value = cachedPopularBooks
            return
        }

        _selectedCategory.value = "Search"
        _booksState.value = emptyList()
        paginatedMemoryCache.clear() // CRITICAL FIX: Clear before searching
        nextPageUrl = null
        _error.value = null
        contentJob?.cancel()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Searching for '$query'..."

            repository.searchBooks(query)
                .onSuccess { result ->
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value =
                        if (result.books.isEmpty()) "No results for '$query'" else "Results for '$query'"
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    private fun syncPopularFeed() {
        viewModelScope.launch {
            val cached = repository.getPopularBooksFlow().firstOrNull()
            if (cached.isNullOrEmpty()) {
                _isLoading.value = true
            }
            repository.syncPopularFeed()
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        val url = nextPageUrl
        if (url == null || _isPaginating.value || _isLoading.value) return

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            _isPaginating.value = true

            repository.getNextPage(url)
                .onSuccess { result ->
                    // CRITICAL FIX: Save to memory cache so DB emissions don't overwrite it
                    paginatedMemoryCache.addAll(result.books)
                    _booksState.value = _booksState.value + result.books
                    nextPageUrl = result.nextPageUrl
                }
                .onFailure { /* Keep existing list */ }

            _isPaginating.value = false
        }
    }

    private fun handleError(e: Throwable) {
        _error.value = e.localizedMessage
        _displayTitle.value = "Something went wrong"
        _isLoading.value = false
    }
}