package com.yugentech.quill.sources.shared

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.localization.UiText
import com.yugentech.quill.util.toUserFriendlyMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What a source screen (Standard Ebooks, Project Gutenberg) shows below its header. The view
 * model decides it and the screen draws it as it is, so the two can never disagree, and there's
 * no moment where a half-updated set of flags reads as the wrong state.
 */
sealed interface SourceContent {

    /**
     * Just opened, cache not read yet: nothing is drawn. The cache answers within a frame or
     * two, so starting on the skeleton would only flash it before the cached books replace it.
     */
    data object Pending : SourceContent

    /** Nothing to show and books are on their way: the skeleton grid. */
    data object Loading : SourceContent

    /**
     * The books to show (never empty: an empty list is [Empty]). [listKey] names the list they
     * belong to (New Arrivals, a category, a search), so the grid can tell a refresh or a new
     * page of the same list, which updates in place, from a different list, which starts afresh.
     */
    data class Books(val listKey: String, val books: List<Book>) : SourceContent

    /** Loaded fine, but there's nothing here (a category with no books, say). */
    data class Empty(val message: UiText) : SourceContent

    /** A search that found nothing: the same no-results state as every other search. */
    data object NoSearchResults : SourceContent

    /** Couldn't load, and there's nothing cached to show instead. */
    data class Error(val message: UiText) : SourceContent

    /** The source's server is down for maintenance, back after [backAfter] if it said when. */
    data class Maintenance(val backAfter: Instant?) : SourceContent

    companion object {
        /**
         * The content for [books] while a refresh is [refreshing] or has ended in [failure]:
         * books win whenever there are any (a failed refresh just leaves the cached ones up), then
         * the skeleton while one is on its way, then maintenance or the error, then [emptyMessage]
         * (or, for a [search], the no-results state).
         */
        fun of(
            listKey: String,
            books: List<Book>,
            emptyMessage: UiText,
            refreshing: Boolean = false,
            failure: Throwable? = null,
            search: Boolean = false
        ): SourceContent = when {
            books.isNotEmpty() -> Books(listKey, books)
            refreshing -> Loading
            failure is ServerMaintenanceException -> Maintenance(failure.backAfter)
            failure != null -> Error(failure.toUserFriendlyMessage())
            search -> NoSearchResults
            else -> Empty(emptyMessage)
        }
    }
}

/**
 * The standard cache-then-network load for a list kept in the database: shows the cached books
 * straight away (and follows them as they change) while [refresh] fetches fresh ones in the
 * background. [onUpdate] gets the books, whether the refresh is still running, and how it failed.
 *
 * When the refresh finishes the cache is read again before reporting it done. The database hands
 * the freshly written books to the collector a moment later, so without this the screen would
 * see "done, no books" for an instant and flash its empty state.
 *
 * With no [refresh] it only follows the cache. Runs until the calling job is cancelled, which is
 * how switching to another list stops it.
 */
suspend fun loadCachedThenRefresh(
    cache: Flow<List<Book>>,
    refresh: (suspend () -> Result<*>)?,
    onUpdate: (books: List<Book>, refreshing: Boolean, failure: Throwable?) -> Unit
) = coroutineScope {
    var refreshing = refresh != null
    var failure: Throwable? = null

    launch { cache.collect { books -> onUpdate(books, refreshing, failure) } }

    if (refresh != null) {
        val result = refresh()
        // Cancelled meanwhile (another list was picked): this result is for a list no longer shown
        ensureActive()
        refreshing = false
        failure = result.exceptionOrNull()
        onUpdate(cache.first(), false, failure)
    }
}
