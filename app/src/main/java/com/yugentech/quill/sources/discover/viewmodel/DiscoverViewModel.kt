package com.yugentech.quill.sources.discover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.sources.discover.state.DiscoverUiState
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.sources.standard.repository.StandardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val standardRepository: StandardRepository,
    private val maintenanceMonitor: SourceMaintenanceMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private var hasSelectedCategories = false

    init {
        loadStandardStorefront()
        watchSubjects()
        watchMaintenance()
    }

    /** Every subject Standard Ebooks has, for Browse by subject, in the catalogue's own order. */
    private fun watchSubjects() {
        viewModelScope.launch {
            standardRepository.getCategoriesFlow().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects.distinct()) }
            }
        }
    }

    /**
     * Whether Standard Ebooks is down for maintenance, asked of its site directly. While it is,
     * it's asked again every minute, so Discover comes back by itself once the site does.
     */
    private fun watchMaintenance() {
        viewModelScope.launch { maintenanceMonitor.checkIfDue(BookSource.STANDARD_EBOOKS) }
        viewModelScope.launch {
            maintenanceMonitor.down.collectLatest { down ->
                val maintenance = down[BookSource.STANDARD_EBOOKS]
                _uiState.update {
                    it.copy(isSourceDown = maintenance != null, sourceBackAfter = maintenance?.backAfter)
                }
                while (maintenance != null) {
                    delay(RECHECK_WHILE_DOWN_MILLIS)
                    maintenanceMonitor.check(BookSource.STANDARD_EBOOKS)
                }
            }
        }
    }

    private fun loadStandardStorefront() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true) }

            launch {
                standardRepository.getNewReleasesFlow().collect { books ->
                    if (books.isNotEmpty() && _uiState.value.heroBooks.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                heroBooks = books.take(10),
                                isFeedLoading = false
                            )
                        }
                    }
                }
            }

            if (!hasSelectedCategories) {
                hasSelectedCategories = true

                val genrePool = mapOf(
                    "Philosophy" to "philosophy",
                    "Mystery" to "mystery",
                    "Fantasy" to "fantasy",
                    "Adventure" to "adventure",
                    "Science Fiction" to "science-fiction",
                    "Horror" to "horror",
                    "Comedy" to "comedy",
                    "Drama" to "drama",
                    "Biography" to "biography",
                    "Poetry" to "poetry",
                    "Satire" to "satire",
                    "Memoir" to "memoir",
                    "Autobiography" to "autobiography",
                    "Spirituality" to "spirituality"
                )

                val displayCategories = genrePool.keys.shuffled().take(5)

                _uiState.update { state ->
                    state.copy(categoryRows = displayCategories.associateWith { emptyList() })
                }

                displayCategories.forEach { categoryName ->
                    val slug = genrePool[categoryName] ?: categoryName.lowercase()
                    launch { standardRepository.syncTopicBooks(slug) }

                    launch {
                        standardRepository.getTopicBooksFlow(slug).collect { categoryBooks ->
                            if (categoryBooks.isNotEmpty() && _uiState.value.categoryRows[categoryName].isNullOrEmpty()) {
                                _uiState.update { state ->
                                    val updatedMap = LinkedHashMap(state.categoryRows)
                                    updatedMap[categoryName] = categoryBooks.take(15)
                                    state.copy(categoryRows = updatedMap)
                                }
                            }
                        }
                    }
                }
            }

            launch { standardRepository.syncNewReleases() }
            launch { standardRepository.syncCategories() }
        }
    }

    fun onSearchQuery(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        _uiState.update {
            it.copy(
                isSearchActive = true,
                isSearchLoading = true,
                searchResults = emptyList()
            )
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Wait for a pause in typing, so each keystroke doesn't start (and then abandon) a
            // search.
            delay(SEARCH_DEBOUNCE_MILLIS)

            val books = standardRepository.searchBooks(query).getOrNull()?.books ?: emptyList()
            _uiState.update {
                it.copy(
                    searchResults = books.distinctBy { book -> book.id },
                    isSearchLoading = false
                )
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
        const val RECHECK_WHILE_DOWN_MILLIS = 60_000L
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                isSearchActive = false,
                isSearchLoading = false,
                searchResults = emptyList()
            )
        }
    }
}