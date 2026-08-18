package com.yugentech.quill.sources.standard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.standard.repository.StandardRepository
import com.yugentech.quill.util.toUserFriendlyMessage
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

private val BASE_CATEGORY = listOf("New Arrivals")

class StandardViewModel(
    private val standardRepository: StandardRepository,
    private val bookDetailsRepository: BookDetailsRepository
) : ViewModel() {

    private val _booksState = MutableStateFlow<List<Book>>(emptyList())
    val booksState = _booksState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _displayTitle = MutableStateFlow("")
    val displayTitle = _displayTitle.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<StandardNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _selectedCategory = MutableStateFlow("New Arrivals")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow(BASE_CATEGORY)
    val categories = _categories.asStateFlow()

    private var contentJob: Job? = null

    private val sessionCache = mutableMapOf<String, List<Book>>()

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
                _categories.value = BASE_CATEGORY + cachedCategories
            }
        }

        refreshNewReleases()
        syncCategories()
    }

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

    fun onCategorySelected(category: String) {
        if (_selectedCategory.value == category) return

        _selectedCategory.value = category
        _error.value = null
        contentJob?.cancel()

        when (category) {
            "New Arrivals" -> refreshNewReleases()
            else -> loadCategory(category)
        }
    }

    private fun loadCategory(category: String) {
        val slug = category.toSlug()

        sessionCache[slug]?.let { cachedBooks ->
            _booksState.value = cachedBooks
            _displayTitle.value = category
            return
        }

        contentJob = viewModelScope.launch {
            _error.value = null

            val cached = standardRepository.getTopicBooksFlow(slug).firstOrNull() ?: emptyList()
            _booksState.value = cached

            if (cached.isEmpty()) {
                _isLoading.value = true
                _displayTitle.value = "Browsing $category..."
            } else {
                _displayTitle.value = category
            }

            standardRepository.syncTopicBooks(slug)
                .onSuccess {
                    val freshBooks = standardRepository.getTopicBooksFlow(slug).firstOrNull() ?: emptyList()
                    _booksState.value = freshBooks
                    _displayTitle.value = if (freshBooks.isEmpty()) "No books found" else category
                    sessionCache[slug] = freshBooks
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    fun onSearchQuery(query: String) {
        if (query.isBlank()) {
            onCategorySelected("New Arrivals")
            return
        }
        val cacheKey = "Search: $query"
        _selectedCategory.value = cacheKey

        sessionCache[cacheKey]?.let { cachedBooks ->
            _booksState.value = cachedBooks
            _displayTitle.value = "Results for '$query'"
            return
        }

        _booksState.value = emptyList()
        performSearch(query = query, cacheKey = cacheKey)
    }

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

            standardRepository.syncNewReleases()
                .onSuccess {
                    _displayTitle.value = "Latest additions"
                }
                .onFailure {
                    _displayTitle.value = "Showing offline cache"
                }

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
                    _displayTitle.value =
                        if (result.books.isEmpty()) "No books found for '$query'" else "Results for '$query'"
                    sessionCache[cacheKey] = result.books
                }
                .onFailure { handleError(it) }

            _isLoading.value = false
        }
    }

    private fun handleError(e: Throwable) {
        if (e is kotlinx.coroutines.CancellationException) return
        _error.value = e.toUserFriendlyMessage()
        _displayTitle.value = "Something went wrong"
    }
}

private fun String.toSlug(): String {
    return this.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
}
