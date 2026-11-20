package com.yugentech.quill.sources.gutenberg.repository

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.mapper.GutenbergOpdsMapper
import com.yugentech.quill.sources.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.sources.shared.ServerMaintenanceException
import kotlinx.coroutines.flow.Flow

interface GutenbergRepository {
    fun getPopularBooksFlow(): Flow<List<Book>>
    /** First page of results; later pages come through [getNextPage] with the result's link. */
    suspend fun searchBooks(query: String): Result<GutenbergFeedResult>
    suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult>
    suspend fun syncPopularFeed(): Result<String?>

    /**
     * Asks gutenberg.org whether it's down: its maintenance if so, or null if it answered and is
     * up. A failure means it couldn't be asked (offline, a timeout), which says neither.
     */
    suspend fun checkMaintenance(): Result<ServerMaintenanceException?>

    /** The summary and subjects a search result lacks, from the book's own gutenberg.org feed. */
    suspend fun getBookExtras(bookId: String): Result<GutenbergOpdsMapper.BookExtras>

    /** Extras already fetched this session, without waiting; null if not fetched yet. */
    fun cachedBookExtras(bookId: String): GutenbergOpdsMapper.BookExtras?

    /** Fetches extras for search results ahead of a tap, so their details open complete. */
    suspend fun prefetchBookExtras(books: List<Book>)
}