package com.yugentech.quill.sources.gutenberg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class GutenbergNavigationEvent {
    data class NavigateByContent(val book: Book) : GutenbergNavigationEvent()
}

class GutenbergViewModel(
    private val repository: GutenbergRepository
) : ViewModel() {

    private val _booksState = MutableStateFlow<List<Book>>(emptyList())
    val booksState = _booksState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private val _displayTitle = MutableStateFlow("Popular Books")
    val displayTitle = _displayTitle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<GutenbergNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var nextPageUrl: String? = null
    private var contentJob: Job? = null
    private var paginationJob: Job? = null

    private var cachedPopularBooks = emptyList<Book>()
    private var paginatedMemoryCache = mutableListOf<Book>()

    init {
        viewModelScope.launch {
            repository.getPopularBooksFlow().collect { cachedBooks ->
                cachedPopularBooks = cachedBooks
                if (_displayTitle.value == "Popular Books") {
                    _booksState.value = cachedBooks + paginatedMemoryCache
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

    fun onSearchQuery(query: String) {
        if (query.isBlank()) {
            paginatedMemoryCache.clear()
            _displayTitle.value = "Popular Books"
            _booksState.value = cachedPopularBooks
            nextPageUrl = null
            return
        }

        _booksState.value = emptyList()
        paginatedMemoryCache.clear()
        nextPageUrl = null
        _error.value = null
        contentJob?.cancel()

        contentJob = viewModelScope.launch {
            _isLoading.value = true
            _displayTitle.value = "Searching for '$query'..."

            repository.searchBooks(query)
                .onSuccess { result ->
                    Timber.d("Search success — ${result.books.size} books, nextUrl=${result.nextPageUrl}")
                    _booksState.value = result.books
                    nextPageUrl = result.nextPageUrl
                    _displayTitle.value =
                        if (result.books.isEmpty()) "No results for '$query'" else "Results for '$query'"
                }
                .onFailure {
                    Timber.e(it, "Search failed")
                    handleError(it)
                }

            _isLoading.value = false
        }
    }

    private fun syncPopularFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            Timber.d("syncPopularFeed() started")
            repository.syncPopularFeed()
                .onSuccess { url ->
                    nextPageUrl = url
                    Timber.d("syncPopularFeed() succeeded — nextPageUrl=$url")
                }
                .onFailure { Timber.e(it, "syncPopularFeed() failed") }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        val url = nextPageUrl
        Timber.d("loadNextPage() called — url=$url, isPaginating=${_isPaginating.value}, isLoading=${_isLoading.value}")
        if (url == null || _isPaginating.value || _isLoading.value) {
            Timber.d("loadNextPage() bailed — url null=${url == null}, isPaginating=${_isPaginating.value}, isLoading=${_isLoading.value}")
            return
        }

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            _isPaginating.value = true
            Timber.d("Paginating — fetching next page from: $url")

            repository.getNextPage(url)
                .onSuccess { result ->
                    Timber.d("Pagination success — got ${result.books.size} books, nextUrl=${result.nextPageUrl}")
                    paginatedMemoryCache.addAll(result.books)
                    _booksState.value += result.books
                    nextPageUrl = result.nextPageUrl
                }
                .onFailure {
                    Timber.e(it, "Pagination failed")
                }

            _isPaginating.value = false
        }
    }

    private fun handleError(e: Throwable) {
        _error.value = e.localizedMessage
        _displayTitle.value = "Something went wrong"
        _isLoading.value = false
    }
}