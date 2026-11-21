package com.yugentech.quill.sources.discover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.R
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.localization.UiText
import com.yugentech.quill.sources.shared.SourceContent
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.sources.shared.loadCachedThenRefresh
import com.yugentech.quill.sources.standard.repository.StandardRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The shelf of the newest books, as opposed to a subject's. */
const val NEW_ARRIVALS = "New Arrivals"

sealed interface ShelfNavigationEvent {
    data class NavigateById(val id: String) : ShelfNavigationEvent
    data class NavigateByContent(val book: Book) : ShelfNavigationEvent
}

/**
 * One whole Discover shelf, opened from its "See all" or from Browse by subject: [shelf] is
 * [NEW_ARRIVALS] or a subject's English name. Cache first, refreshed from the network behind it.
 */
class ShelfViewModel(
    val shelf: String,
    private val standardRepository: StandardRepository,
    private val bookDetailsRepository: BookDetailsRepository,
    private val maintenanceMonitor: SourceMaintenanceMonitor
) : ViewModel() {

    // Blank until the cache answers, so opening doesn't flash the skeleton before cached books
    private val _content = MutableStateFlow<SourceContent>(SourceContent.Pending)
    val content: StateFlow<SourceContent> = _content.asStateFlow()

    // One-off navigation, buffered so an event sent while the screen isn't listening isn't lost
    private val _navigationEvents = Channel<ShelfNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<ShelfNavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch { maintenanceMonitor.checkIfDue(BookSource.STANDARD_EBOOKS) }
        viewModelScope.launch {
            val noBooks = UiText.Res(R.string.no_books_found)
            if (shelf == NEW_ARRIVALS) {
                loadCachedThenRefresh(
                    cache = standardRepository.getNewReleasesFlow(),
                    refresh = { standardRepository.syncNewReleases() }
                ) { books, refreshing, failure ->
                    _content.value = SourceContent.of(shelf, books, noBooks, refreshing, failure)
                }
            } else {
                val slug = shelf.toSubjectSlug()
                loadCachedThenRefresh(
                    cache = standardRepository.getTopicBooksFlow(slug),
                    refresh = { standardRepository.syncTopicBooks(slug) }
                ) { books, refreshing, failure ->
                    _content.value = SourceContent.of(shelf, books, noBooks, refreshing, failure)
                }
            }
        }
    }

    fun onBookClick(book: Book) {
        viewModelScope.launch {
            val event = if (bookDetailsRepository.isBookInLibrary(book.id)) {
                ShelfNavigationEvent.NavigateById(book.id)
            } else {
                ShelfNavigationEvent.NavigateByContent(book)
            }
            _navigationEvents.send(event)
        }
    }
}

/** A subject's name as Standard Ebooks writes it in its addresses ("Science Fiction" → "science-fiction"). */
fun String.toSubjectSlug(): String = lowercase()
    .replace(Regex("[^a-z0-9\\s-]"), "")
    .replace(Regex("\\s+"), "-")
    .replace(Regex("-+"), "-")
    .trim('-')
