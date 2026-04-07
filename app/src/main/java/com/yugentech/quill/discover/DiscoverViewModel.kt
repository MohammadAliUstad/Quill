package com.yugentech.quill.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.standardEBooks.repository.StandardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

                val genrePool = listOf(
                    "Philosophy", "Mystery", "Fantasy", "Adventure", "Science Fiction",
                    "Horror", "Comedy", "Drama", "Biography", "Poetry",
                    "Satire", "Memoir", "Autobiography", "Spirituality"
                )

                val displayCategories = genrePool.shuffled().take(5)

                _uiState.update { state ->
                    state.copy(categoryRows = displayCategories.associateWith { emptyList() })
                }

                displayCategories.forEach { category ->
                    launch { standardRepository.syncTopicBooks(category) }

                    launch {
                        standardRepository.getTopicBooksFlow(category).collect { categoryBooks ->
                            if (categoryBooks.isNotEmpty() && _uiState.value.categoryRows[category].isNullOrEmpty()) {
                                _uiState.update { state ->
                                    val updatedMap = LinkedHashMap(state.categoryRows)
                                    updatedMap[category] = categoryBooks.take(15)
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
            val standardDeferred = async { standardRepository.searchBooks(query) }
            val gutenbergDeferred = async { gutenbergRepository.searchBooks(query) }

            val standardResult = standardDeferred.await().getOrNull()?.books ?: emptyList()
            val gutenbergResult = gutenbergDeferred.await().getOrNull()?.books ?: emptyList()

            _uiState.update {
                it.copy(
                    searchResults = standardResult + gutenbergResult,
                    isSearchLoading = false
                )
            }
        }
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