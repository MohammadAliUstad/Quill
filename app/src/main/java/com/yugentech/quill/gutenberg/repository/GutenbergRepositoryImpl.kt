package com.yugentech.quill.gutenberg.repository

import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.dao.CategoryCacheDao
import com.yugentech.quill.database.entity.CategoryCacheEntity
import com.yugentech.quill.database.mapper.toCatalogEntity
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.gutenberg.mapper.GutenbergMapper
import com.yugentech.quill.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.gutenberg.service.GutenbergApiService
import com.yugentech.quill.database.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private const val SOURCE_KEY = "gutenberg"
private const val FEED_KEY = "gutenberg-popular"

class GutenbergRepositoryImpl(
    private val apiService: GutenbergApiService,
    private val catalogDao: CatalogDao,
    private val categoryCacheDao: CategoryCacheDao,
) : GutenbergRepository {

    // ── Cache-backed flows ────────────────────────────────────────────────────

    override fun getPopularBooksFlow(): Flow<List<Book>> {
        return catalogDao.getBooksByCategory(FEED_KEY).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCategoriesFlow(): Flow<List<String>> {
        return categoryCacheDao.getCategoriesBySource(SOURCE_KEY)
    }

    override fun getTopicBooksFlow(topic: String): Flow<List<Book>> {
        return catalogDao.getBooksByCategory("gutenberg-topic-$topic").map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // ── Background sync — single network call for both books + categories ─────

    override suspend fun syncPopularFeed(): Result<Unit> = runCatching {
        val json = apiService.getPopularBooks(page = 1)

        // Parse and cache popular books
        val result = GutenbergMapper.parseFeed(json)
        if (result.books.isNotEmpty()) {
            val newEntities = result.books.map { it.toCatalogEntity(FEED_KEY) }
            val newIds = newEntities.map { it.id }.toSet()
            catalogDao.insertBooks(newEntities)
            catalogDao.deleteStaleBooks(FEED_KEY, newIds.toList())
        }

        // Parse and cache categories from the same response
        val categories = GutenbergMapper.parseCategoriesFromFeed(json)
        if (categories.isNotEmpty()) {
            categoryCacheDao.clearCategories(SOURCE_KEY)
            categoryCacheDao.insertCategories(
                categories.map { CategoryCacheEntity(name = it.name, source = SOURCE_KEY) }
            )
        }
    }.also { if (it.isFailure) Timber.w(it.exceptionOrNull(), "Gutenberg feed sync failed") }

    override suspend fun syncTopicBooks(topic: String): Result<Unit> = runCatching {
        val feedKey = "gutenberg-topic-$topic"
        val result = GutenbergMapper.parseFeed(apiService.getBooksByTopic(topic, page = 1))
        if (result.books.isNotEmpty()) {
            val newEntities = result.books.map { it.toCatalogEntity(feedKey) }
            val newIds = newEntities.map { it.id }.toSet()
            catalogDao.insertBooks(newEntities)
            catalogDao.deleteStaleBooks(feedKey, newIds.toList())
        }
    }.also { if (it.isFailure) Timber.w(it.exceptionOrNull(), "Gutenberg topic sync failed: $topic") }

    // ── Network-only ──────────────────────────────────────────────────────────

    override suspend fun searchBooks(query: String, page: Int): Result<GutenbergFeedResult> =
        runCatching { GutenbergMapper.parseFeed(apiService.searchBooks(query, page)) }

    override suspend fun getBooksByTopic(topic: String, page: Int): Result<GutenbergFeedResult> =
        runCatching { GutenbergMapper.parseFeed(apiService.getBooksByTopic(topic, page)) }

    override suspend fun getBooksByAuthor(authorName: String, page: Int): Result<GutenbergFeedResult> =
        runCatching { GutenbergMapper.parseFeed(apiService.getBooksByAuthor(authorName, page)) }

    override suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult> =
        runCatching { GutenbergMapper.parseFeed(apiService.getNextPage(nextUrl)) }

    override suspend fun getBookById(id: Int): Result<Book> = runCatching {
        GutenbergMapper.parseSingleBook(apiService.getBookById(id))
            ?: error("Failed to parse book $id")
    }
}