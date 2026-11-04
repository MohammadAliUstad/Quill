package com.yugentech.quill.sources.discover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.discover.state.DiscoverUiState
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.sources.standard.repository.StandardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val standardRepository: StandardRepository,
    private val gutenbergRepository: GutenbergRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private var hasSelectedCategories = false

    init {
        loadStandardStorefront()
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
            // search on both sites.
            delay(SEARCH_DEBOUNCE_MILLIS)

            // Each source's books go into the grid as soon as that source answers, after any
            // already there, so a slower source never holds the other's results back and the
            // grid only grows at the end. The spinner stays only until something arrives.
            val results = mutableListOf<Book>()
            var pending = 2
            val publish: (List<Book>) -> Unit = { books ->
                results += books.filter { book -> results.none { it.id == book.id } }
                pending--
                _uiState.update {
                    it.copy(
                        searchResults = results.toList(),
                        isSearchLoading = pending > 0 && results.isEmpty()
                    )
                }
            }

            launch { publish(standardRepository.searchBooks(query).getOrNull()?.books ?: emptyList()) }
            launch {
                val books = gutenbergRepository.searchBooks(query).getOrNull()?.books ?: emptyList()
                publish(books)
                // Gutenberg results lack their summaries; fetch the top ones now, so tapping one
                // opens a complete page.
                gutenbergRepository.prefetchBookExtras(books.take(PREFETCH_COUNT))
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
        const val PREFETCH_COUNT = 8
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