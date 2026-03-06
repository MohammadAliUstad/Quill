package com.yugentech.quill.ui.mainScreen.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.standardEBooks.repository.StandardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val heroBooks: List<Book> = emptyList(),
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

    private val _gutenbergState = MutableStateFlow(DiscoverUiState())

    // Combines the cached Standard flow with Gutenberg state reactively
    val uiState: StateFlow<DiscoverUiState> = combine(
        standardRepository.getNewReleasesFlow(),
        gutenbergRepository.getPopularBooksFlow(),
        _gutenbergState
    ) { standardBooks, cachedPopular, gutenbergState ->

        // Merge cached popular with any freshly fetched popular
        val popularBooks = cachedPopular.ifEmpty { gutenbergState.gutenbergPopular }

        val heroBooks = interleave(
            standardBooks.take(3),
            popularBooks.take(3)
        ).distinctBy { it.id }.take(6)

        val heroIds = heroBooks.map { it.id }.toSet()

        gutenbergState.copy(
            heroBooks = heroBooks,
            standardNewReleases = standardBooks.filter { it.id !in heroIds },
            gutenbergPopular = popularBooks.filter { it.id !in heroIds }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiscoverUiState()
    )

    init {
        syncData()
    }

    private fun syncData() {
        viewModelScope.launch {
            // Sync Standard cache in background — Flow above updates automatically
            launch { standardRepository.syncNewReleases() }

            // Sync Gutenberg popular cache in background — Flow above updates automatically
            launch { gutenbergRepository.syncPopularFeed() }

            // Fetch genre shelves in parallel — these are network-only for now
            val sciFi = async {
                gutenbergRepository.getBooksByTopic("Science Fiction").getOrNull()?.books
                    ?: emptyList()
            }
            val mystery = async {
                gutenbergRepository.getBooksByTopic("Mystery").getOrNull()?.books
                    ?: emptyList()
            }
            val romance = async {
                gutenbergRepository.getBooksByTopic("Romance").getOrNull()?.books
                    ?: emptyList()
            }

            // All three complete together — set loading false only when shelves are ready
            _gutenbergState.value = _gutenbergState.value.copy(
                sciFiBooks = sciFi.await(),
                mysteryBooks = mystery.await(),
                romanceBooks = romance.await(),
                isLoading = false
            )
        }
    }

    private fun <T> interleave(a: List<T>, b: List<T>): List<T> {
        val result = mutableListOf<T>()
        val maxSize = maxOf(a.size, b.size)
        for (i in 0 until maxSize) {
            if (i < a.size) result.add(a[i])
            if (i < b.size) result.add(b[i])
        }
        return result
    }
}