package com.yugentech.quill.ui.dash.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.gutenberg.GutenbergRepository
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.standardEBooks.StandardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val heroBook: Book? = null,
    val standardNewReleases: List<Book> = emptyList(),
    val gutenbergPopular: List<Book> = emptyList(),
    val sciFiBooks: List<Book> = emptyList(),
    val mysteryBooks: List<Book> = emptyList(),
    val romanceBooks: List<Book> = emptyList(),
    val isLoading: Boolean = true
)

class DiscoverViewModel(
    private val standardRepository: StandardRepository,
    private val gutenbergRepository: GutenbergRepository
) : ViewModel() {

    private val _manualRefreshTrigger = MutableStateFlow(0)
    
    // We combine the flow from Standard Repo with our manual Gutenberg fetches
    private val _gutenbergState = MutableStateFlow(DiscoverUiState())

    val uiState: StateFlow<DiscoverUiState> = combine(
        standardRepository.getNewReleasesFlow(),
        _gutenbergState
    ) { standardBooks, gutenbergState ->
        gutenbergState.copy(
            standardNewReleases = standardBooks,
            // If we haven't picked a hero yet and we have data, pick one
            heroBook = gutenbergState.heroBook ?: standardBooks.firstOrNull() ?: gutenbergState.gutenbergPopular.firstOrNull()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiscoverUiState()
    )

    init {
        fetchGutenbergData()
        refreshStandardData()
    }

    private fun refreshStandardData() {
        viewModelScope.launch {
            standardRepository.syncNewReleases()
        }
    }

    private fun fetchGutenbergData() {
        viewModelScope.launch {
            // Load in parallel
            launch {
                gutenbergRepository.getPopularBooks(1).onSuccess {
                    updateGutenbergState { copy(gutenbergPopular = it.books) }
                }
            }
            launch {
                gutenbergRepository.getBooksByTopic("Science Fiction", 1).onSuccess {
                    updateGutenbergState { copy(sciFiBooks = it.books) }
                }
            }
            launch {
                gutenbergRepository.getBooksByTopic("Mystery", 1).onSuccess {
                    updateGutenbergState { copy(mysteryBooks = it.books) }
                }
            }
            launch {
                gutenbergRepository.getBooksByTopic("Romance", 1).onSuccess {
                    updateGutenbergState { copy(romanceBooks = it.books) }
                }
            }
            // Once initial fetches are fired, turn off global loading
            _gutenbergState.value = _gutenbergState.value.copy(isLoading = false)
        }
    }

    private fun updateGutenbergState(update: DiscoverUiState.() -> DiscoverUiState) {
        _gutenbergState.value = _gutenbergState.value.update()
    }
}