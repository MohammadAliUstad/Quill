package com.yugentech.quill.sources.gutenberg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.R
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.localization.UiText
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.sources.shared.SourceContent
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.sources.shared.loadCachedThenRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class GutenbergNavigationEvent {
    data class NavigateById(val id: String) : GutenbergNavigationEvent()
    data class NavigateByContent(val book: Book) : GutenbergNavigationEvent()
}

/** Everything the Project Gutenberg screen shows, in one place, updated all at once. */
data class GutenbergUiState(
    // Blank until the cache answers, so opening doesn't flash the skeleton before cached books
    val content: SourceContent = SourceContent.Pending,
    // The next page is on its way: a row of placeholder covers under the grid
    val isPaginating: Boolean = false,
    // More pages to come: the grid holds back its last, possibly partial, row until they do
    val hasMorePages: Boolean = false
)

/**
 * Project Gutenberg: the popular feed and search, both paged.
 *
 * One list is on screen at a time, loaded by one job: searching (or clearing a search) cancels
 * the job and any page still loading, so nothing from the list being left lands on the new one.
 * The popular feed's first page is kept in the database and shown cache first, refreshed from
 * the network once per session; its later pages, and a search's, are kept in memory.
 */
class GutenbergViewModel(
    private val gutenbergRepository: GutenbergRepository,
    private val bookDetailsRepository: BookDetailsRepository,
    private val maintenanceMonitor: SourceMaintenanceMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(GutenbergUiState())
    val uiState: StateFlow<GutenbergUiState> = _uiState.asStateFlow()

    // One-off navigation, buffered so an event sent while the screen isn't listening isn't lost
    private val _navigationEvents = Channel<GutenbergNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<GutenbergNavigationEvent> = _navigationEvents.receiveAsFlow()

    /** The search showing, or null for the popular feed. */
    private var query: String? = null

    /** Names the list showing, for the grid: the popular feed, or a search. */
    private val listKey: String get() = query?.let { "Search: $it" } ?: "Popular"

    /** The list's first page (the cached popular books, or a search's first results)… */
    private var firstPage: List<Book> = emptyList()

    /** …and the pages loaded beneath it since. */
    private val laterPages = mutableListOf<Book>()

    private var nextPageUrl: String? = null
        set(value) {
            field = value
            _uiState.update { it.copy(hasMorePages = value != null) }
        }

    // The popular feed's own next page and whether it's been refreshed this session, so clearing
    // a search goes back to it without fetching it again
    private var popularNextPageUrl: String? = null
    private var popularRefreshed = false

    private var contentJob: Job? = null
    private var pageJob: Job? = null

    init {
        // Its covers and downloads are on gutenberg.org, which can be down while the list loads
        viewModelScope.launch { maintenanceMonitor.checkIfDue(BookSource.GUTENBERG) }
        showPopular()
    }

    fun onBookClick(book: Book) {
        viewModelScope.launch {
            val event = if (bookDetailsRepository.isBookInLibrary(book.id)) {
                GutenbergNavigationEvent.NavigateById(book.id)
            } else {
                GutenbergNavigationEvent.NavigateByContent(book)
            }
            _navigationEvents.send(event)
        }
    }

    fun onSearchQuery(newQuery: String) {
        val trimmed = newQuery.trim().ifEmpty { null }
        // Closing an empty search over the popular feed changes nothing: keep its loaded pages
        if (trimmed == query) return
        if (trimmed == null) showPopular() else showSearch(trimmed)
    }

    fun loadNextPage() {
        val url = nextPageUrl ?: return
        val state = _uiState.value
        if (state.isPaginating || state.content !is SourceContent.Books) return

        pageJob = viewModelScope.launch {
            _uiState.update { it.copy(isPaginating = true) }
            val result = gutenbergRepository.getNextPage(url)
            ensureActive()
            result
                .onSuccess { page ->
                    Timber.d("Page loaded: ${page.books.size} books, next=${page.nextPageUrl}")
                    laterPages += page.books
                    nextPageUrl = page.nextPageUrl
                }
                .onFailure {
                    Timber.e(it, "Loading the next page failed")
                    // Stop paging this list: the grid shows its last row and doesn't ask for the
                    // failing page over and over. The popular feed keeps its own next page, so
                    // coming back to it (after a search) tries again.
                    nextPageUrl = null
                }
            // The new books and the end of paging arrive together
            _uiState.update {
                it.copy(content = SourceContent.Books(listKey, firstPage + laterPages), isPaginating = false)
            }
        }
    }

    /** The popular feed: cache first, refreshed from the network the first time it's shown. */
    private fun showPopular() {
        resetList(newQuery = null, nextPage = popularNextPageUrl)
        val refresh: (suspend () -> Result<*>)? = if (popularRefreshed) null else {
            {
                gutenbergRepository.syncPopularFeed().onSuccess { url ->
                    popularRefreshed = true
                    popularNextPageUrl = url
                    // A search may have started meanwhile; don't hand it the popular feed's page
                    if (query == null) nextPageUrl = url
                }
            }
        }
        launchContent {
            loadCachedThenRefresh(gutenbergRepository.getPopularBooksFlow(), refresh) { books, refreshing, failure ->
                firstPage = books
                setContent(SourceContent.of(listKey, books + laterPages, NoBooks, refreshing, failure))
            }
        }
    }

    /** A search: the skeleton straight away, then its first page of results. */
    private fun showSearch(searchQuery: String) {
        resetList(newQuery = searchQuery, nextPage = null)
        setContent(SourceContent.Loading)
        val noResults = UiText.Res(R.string.no_results_for, searchQuery)
        launchContent {
            val result = gutenbergRepository.searchBooks(searchQuery)
            ensureActive()
            result
                .onSuccess { feed ->
                    firstPage = feed.books
                    nextPageUrl = feed.nextPageUrl
                    setContent(SourceContent.of(listKey, feed.books, noResults, search = true))
                    // Search results lack their summaries; fetch the top ones now, so tapping one
                    // opens a complete page
                    gutenbergRepository.prefetchBookExtras(feed.books.take(8))
                }
                .onFailure {
                    Timber.e(it, "Search failed")
                    setContent(SourceContent.of(listKey, emptyList(), noResults, failure = it))
                }
        }
    }

    /** Leaves the current list: its loads stop, its pages go, and paging starts over. */
    private fun resetList(newQuery: String?, nextPage: String?) {
        contentJob?.cancel()
        pageJob?.cancel()
        query = newQuery
        firstPage = emptyList()
        laterPages.clear()
        nextPageUrl = nextPage
        _uiState.update { it.copy(isPaginating = false) }
    }

    private fun launchContent(block: suspend CoroutineScope.() -> Unit) {
        contentJob = viewModelScope.launch(block = block)
    }

    private fun setContent(content: SourceContent) = _uiState.update { it.copy(content = content) }

    private companion object {
        val NoBooks = UiText.Res(R.string.no_books_found)
    }
}
