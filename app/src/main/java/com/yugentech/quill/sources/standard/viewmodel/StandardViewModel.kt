package com.yugentech.quill.sources.standard.viewmodel

import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.database.model.BookSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.R
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.localization.UiText
import com.yugentech.quill.sources.shared.SourceContent
import com.yugentech.quill.sources.shared.loadCachedThenRefresh
import com.yugentech.quill.sources.standard.repository.StandardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class StandardNavigationEvent {
    data class NavigateById(val id: String) : StandardNavigationEvent()
    data class NavigateByContent(val book: Book) : StandardNavigationEvent()
}

const val NEW_ARRIVALS = "New Arrivals"

/** Everything the Standard Ebooks screen shows, in one place, updated all at once. */
data class StandardUiState(
    // Blank until the cache answers, so opening doesn't flash the skeleton before cached books
    val content: SourceContent = SourceContent.Pending,
    // Empty until the category cache answers; then New Arrivals and the cached subjects arrive
    // together, so the chip row fades in whole instead of New Arrivals sitting alone first
    val categories: List<String> = emptyList(),
    // The highlighted chip; a search ("Search: …") highlights none
    val selectedCategory: String = NEW_ARRIVALS
)

/**
 * Standard Ebooks: New Arrivals, the subject categories and search.
 *
 * One list is on screen at a time, loaded by one job: picking another cancels the job, so a
 * slower earlier load can never land on the new list. New Arrivals and categories are kept in
 * the database and shown cache first, refreshed from the network behind them. A category's books
 * and each search's results are also kept for the session, so going back to one is instant.
 */
class StandardViewModel(
    private val standardRepository: StandardRepository,
    private val bookDetailsRepository: BookDetailsRepository,
    private val maintenanceMonitor: SourceMaintenanceMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(StandardUiState())
    val uiState: StateFlow<StandardUiState> = _uiState.asStateFlow()

    // One-off navigation, buffered so an event sent while the screen isn't listening isn't lost
    private val _navigationEvents = Channel<StandardNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<StandardNavigationEvent> = _navigationEvents.receiveAsFlow()

    private var contentJob: Job? = null
    private val sessionCache = mutableMapOf<String, List<Book>>()

    init {
        viewModelScope.launch {
            standardRepository.getCategoriesFlow().collect { cached ->
                _uiState.update { it.copy(categories = (listOf(NEW_ARRIVALS) + cached).distinct()) }
            }
        }
        viewModelScope.launch { standardRepository.syncCategories() }
        viewModelScope.launch { maintenanceMonitor.checkIfDue(BookSource.STANDARD_EBOOKS) }
        showNewArrivals()
    }

    fun onBookClick(book: Book) {
        viewModelScope.launch {
            val event = if (bookDetailsRepository.isBookInLibrary(book.id)) {
                StandardNavigationEvent.NavigateById(book.id)
            } else {
                StandardNavigationEvent.NavigateByContent(book)
            }
            _navigationEvents.send(event)
        }
    }

    fun onCategorySelected(category: String) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.update { it.copy(selectedCategory = category) }
        if (category == NEW_ARRIVALS) showNewArrivals() else showCategory(category)
    }

    fun onSearchQuery(query: String) {
        if (query.isBlank()) {
            onCategorySelected(NEW_ARRIVALS)
            return
        }
        val cacheKey = "Search: $query"
        if (_uiState.value.selectedCategory == cacheKey) return
        _uiState.update { it.copy(selectedCategory = cacheKey) }
        val noResults = UiText.Res(R.string.no_books_found_for, query)

        sessionCache[cacheKey]?.let { cached ->
            showNow(SourceContent.of(cacheKey, cached, noResults, search = true))
            return
        }

        // Nothing cached for a new search, so the skeleton goes up straight away
        setContent(SourceContent.Loading)
        launchContent {
            val result = standardRepository.searchBooks(query)
            ensureActive()
            result
                .onSuccess { feed ->
                    sessionCache[cacheKey] = feed.books
                    setContent(SourceContent.of(cacheKey, feed.books, noResults, search = true))
                }
                .onFailure { setContent(SourceContent.of(cacheKey, emptyList(), noResults, failure = it)) }
        }
    }

    /** The newest books, cache first; refreshed each time they're picked. */
    private fun showNewArrivals() {
        launchContent {
            loadCachedThenRefresh(
                cache = standardRepository.getNewReleasesFlow(),
                refresh = { standardRepository.syncNewReleases() }
            ) { books, refreshing, failure ->
                setContent(SourceContent.of(NEW_ARRIVALS, books, NoBooks, refreshing, failure))
            }
        }
    }

    /** A subject: from this session's copy if it's been opened before, else cache first. */
    private fun showCategory(category: String) {
        val slug = category.toSlug()
        val listKey = "Category: $slug"
        sessionCache[slug]?.let { cached ->
            showNow(SourceContent.of(listKey, cached, NoBooks))
            return
        }
        val cache = standardRepository.getTopicBooksFlow(slug)
        launchContent {
            loadCachedThenRefresh(
                cache = cache,
                refresh = {
                    standardRepository.syncTopicBooks(slug)
                        .onSuccess { sessionCache[slug] = cache.first() }
                }
            ) { books, refreshing, failure ->
                setContent(SourceContent.of(listKey, books, NoBooks, refreshing, failure))
            }
        }
    }

    /**
     * Replaces the list's job: whatever was loading for the previous list stops. The previous
     * list stays up until the new one's first books (or the skeleton) replace it, so switching
     * between cached lists doesn't blink through the skeleton.
     */
    private fun launchContent(block: suspend CoroutineScope.() -> Unit) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch(block = block)
    }

    /** Shows [content] at once, with nothing left loading behind it. */
    private fun showNow(content: SourceContent) {
        contentJob?.cancel()
        setContent(content)
    }

    private fun setContent(content: SourceContent) = _uiState.update { it.copy(content = content) }

    private companion object {
        val NoBooks = UiText.Res(R.string.no_books_found)
    }
}

private fun String.toSlug(): String {
    return this.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
}
