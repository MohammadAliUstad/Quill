package com.yugentech.quill.gutenberg.repository

import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.mapper.toCatalogEntity
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.gutenberg.mapper.GutenbergMapper
import com.yugentech.quill.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.gutenberg.service.GutenbergApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

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

    override suspend fun syncPopularFeed(): Result<String?> = runCatching {
        val json = apiService.getPopularBooks(page = 1)
        Timber.d("syncPopularFeed raw JSON length=${json.length}")

        // Log the raw next field directly from JSON before mapper touches it
        val rawNext = try {
            val idx = json.indexOf("\"next\"")
            if (idx != -1) json.substring(idx, minOf(idx + 80, json.length)) else "\"next\" field NOT FOUND in JSON"
        } catch (e: Exception) { "failed to extract next field" }
        Timber.d("syncPopularFeed raw next field: $rawNext")

        val result = GutenbergMapper.parseFeed(json)
        Timber.d("syncPopularFeed parsed — books=${result.books.size}, nextPageUrl=${result.nextPageUrl}")

        if (result.books.isNotEmpty()) {
            val newEntities = result.books.map { it.toCatalogEntity(FEED_KEY) }
            val newIds = newEntities.map { it.id }.toSet()
            catalogDao.insertBooks(newEntities)
            catalogDao.deleteStaleBooks(FEED_KEY, newIds.toList())
            Timber.d("syncPopularFeed DB write done — inserted ${newEntities.size} entities")
        } else {
            Timber.w("syncPopularFeed — parsed 0 books, nothing written to DB")
        }

        result.nextPageUrl
    }.also { if (it.isFailure) Timber.e(it.exceptionOrNull(), "Gutenberg feed sync failed") }

    override suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult> =
        runCatching {
            Timber.d("getNextPage() fetching: $nextUrl")
            GutenbergMapper.parseFeed(apiService.getNextPage(nextUrl)).also {
                Timber.d("getNextPage() parsed — ${it.books.size} books, nextUrl=${it.nextPageUrl}")
            }
        }

    override suspend fun searchBooks(query: String, page: Int): Result<GutenbergFeedResult> =
        runCatching { GutenbergMapper.parseFeed(apiService.searchBooks(query, page)) }
}