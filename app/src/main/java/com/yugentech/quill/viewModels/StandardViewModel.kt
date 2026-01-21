package com.yugentech.quill.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.network.StandardRepository
import com.yugentech.quill.network.domain.Book
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StandardViewModel(
    private val standardRepository: StandardRepository
) : ViewModel() {

    // 1. MAIN CONTENT STATE
    // Holds the books currently displayed in the main grid (New Releases OR Search Results)
    private val _booksState = MutableStateFlow<List<Book>>(emptyList())
    val booksState = _booksState.asStateFlow()

    // 2. UI STATUS
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    // 3. CATEGORY MANAGEMENT
    // The chip currently selected by the user (starts with "New Arrivals")
    private val _selectedCategory = MutableStateFlow("New Arrivals")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Hardcoded list of popular Standard Ebooks categories for the UI Chips
    val categories = listOf(
        "New Arrivals",
        "Sci-Fi",
        "Fantasy",
        "Mystery",
        "Thriller",
        "Philosophy",
        "History",
        "Adventure",
        "Drama",
        "Horror",
        "Short Fiction"
    )

    // Job to cancel previous searches if the user clicks too fast
    private var searchJob: Job? = null

    init {
        // Load the "Hero" content immediately
        loadNewReleases()
    }

    /**
     * Called when the user clicks a Chip (e.g., "Sci-Fi") or the "New Arrivals" button.
     */
    // 1. Map UI names to API slugs
    private fun mapCategoryToSlug(uiCategory: String): String {
        return when (uiCategory) {
            "Sci-Fi" -> "science-fiction"
            "Shorts" -> "shorts"
            "Children's" -> "childrens"
            "New Arrivals" -> "new-releases" // Special case
            else -> uiCategory.lowercase() // Default: "Drama" -> "drama"
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        val slug = mapCategoryToSlug(category) // <--- USE THIS

        if (slug == "new-releases") {
            loadNewReleases()
        } else {
            // Use the slug for the API call
            performSearch(query = slug, isCategorySearch = true)
        }
    }

    /**
     * Called when the user types in the actual Search Bar.
     */
    fun onSearchQuery(query: String) {
        if (query.isBlank()) return
        _selectedCategory.value = "Search" // Deselect other chips
        performSearch(query = query, isCategorySearch = false)
    }

    /**
     * Fetches the fast, lightweight list of recent books.
     */
    private fun loadNewReleases() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Fetching new releases..."

            standardRepository.getNewReleases()
                .onSuccess { books ->
                    _booksState.value = books
                    _isLoading.value = false
                    _statusMessage.value = "Latest additions"
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _booksState.value = emptyList()
                    _statusMessage.value = "Failed to load: ${e.localizedMessage}"
                }
        }
    }

    /**
     * Unified search logic for both Categories and explicit User Search.
     */
    private fun performSearch(query: String, isCategorySearch: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = if (isCategorySearch) "Browsing $query..." else "Searching for '$query'..."

            standardRepository.searchBooks(query)
                .onSuccess { books ->
                    _booksState.value = books
                    _isLoading.value = false

                    if (books.isEmpty()) {
                        _statusMessage.value = "No books found for '$query'"
                    } else {
                        _statusMessage.value = if (isCategorySearch) "$query" else "Results for '$query'"
                    }
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _booksState.value = emptyList()
                    _statusMessage.value = "Error: ${e.localizedMessage}"
                }
        }
    }
}