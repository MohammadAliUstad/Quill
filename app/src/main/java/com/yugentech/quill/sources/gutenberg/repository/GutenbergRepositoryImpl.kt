package com.yugentech.quill.sources.gutenberg.repository

import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.mapper.toCatalogEntity
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.mapper.GutenbergMapper
import com.yugentech.quill.sources.gutenberg.mapper.GutenbergOpdsMapper
import com.yugentech.quill.sources.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.sources.gutenberg.service.GutenbergApiService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

private const val FEED_KEY = "gutenberg-popular"

class GutenbergRepositoryImpl(
    private val apiService: GutenbergApiService,
    private val catalogDao: CatalogDao
) : GutenbergRepository {

    override fun getPopularBooksFlow(): Flow<List<Book>> {
        return catalogDao.getBooksByCategory(FEED_KEY).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun syncPopularFeed(): Result<String?> = try {
        val json = apiService.getPopularBooks(page = 1)
        val result = GutenbergMapper.parseFeed(json)

        if (result.books.isNotEmpty()) {
            val newEntities = result.books.map { it.toCatalogEntity(FEED_KEY) }
            val newIds = newEntities.map { it.id }.toSet()
            catalogDao.insertBooks(newEntities)
            catalogDao.deleteStaleBooks(FEED_KEY, newIds.toList())
        }

        Result.success(result.nextPageUrl)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.e(e, "Gutenberg feed sync failed")
        Result.failure(e)
    }

    // The popular grid pages through Gutendex (JSON); search results page through gutenberg.org
    // (OPDS). The next-page link says which it is.
    override suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult> = try {
        val body = apiService.getNextPage(nextUrl)
        Result.success(
            if (nextUrl.startsWith(GutenbergOpdsMapper.SITE)) GutenbergOpdsMapper.parseSearchFeed(body)
            else GutenbergMapper.parseFeed(body)
        )
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun searchBooks(query: String): Result<GutenbergFeedResult> = try {
        Result.success(GutenbergOpdsMapper.parseSearchFeed(apiService.searchBooks(query)))
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(e)
    }

    // Summaries fetched this session, so a book opened from search (or opened again) has its
    // summary at once. Requests already running are shared rather than repeated.
    private val extrasCache = ConcurrentHashMap<String, GutenbergOpdsMapper.BookExtras>()
    private val extrasInFlight = ConcurrentHashMap<String, CompletableDeferred<Result<GutenbergOpdsMapper.BookExtras>>>()

    // Prefetching stays gentle on gutenberg.org: a few books at a time.
    private val prefetchPermits = Semaphore(3)

    override fun cachedBookExtras(bookId: String): GutenbergOpdsMapper.BookExtras? = extrasCache[bookId]

    override suspend fun getBookExtras(bookId: String): Result<GutenbergOpdsMapper.BookExtras> {
        extrasCache[bookId]?.let { return Result.success(it) }

        // Someone's already fetching it (usually the prefetch): wait for theirs. If theirs was
        // cancelled (a new search started), fetch it here instead.
        extrasInFlight[bookId]?.let { running ->
            val shared = running.await()
            if (shared.isSuccess) return shared
        }

        val mine = CompletableDeferred<Result<GutenbergOpdsMapper.BookExtras>>()
        extrasInFlight[bookId] = mine
        val result = try {
            val number = GutenbergOpdsMapper.numberOf(bookId)
                ?: throw IllegalArgumentException("$bookId isn't a Gutenberg book")
            Result.success(GutenbergOpdsMapper.parseBookFeed(apiService.getBookFeed(number)))
        } catch (e: Exception) {
            // Anyone waiting on this request gets a failure and fetches for themselves.
            mine.complete(Result.failure(e))
            extrasInFlight.remove(bookId, mine)
            if (e is kotlinx.coroutines.CancellationException) throw e
            return Result.failure(e)
        }
        result.getOrNull()?.let { extrasCache[bookId] = it }
        mine.complete(result)
        extrasInFlight.remove(bookId, mine)
        return result
    }

    override suspend fun prefetchBookExtras(books: List<Book>) = coroutineScope {
        books.filter { it.description == null && !extrasCache.containsKey(it.id) }.forEach { book ->
            launch { prefetchPermits.withPermit { getBookExtras(book.id) } }
        }
    }
}