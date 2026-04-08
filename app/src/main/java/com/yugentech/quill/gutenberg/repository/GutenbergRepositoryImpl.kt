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

        val result = GutenbergMapper.parseFeed(json)

        if (result.books.isNotEmpty()) {
            val newEntities = result.books.map { it.toCatalogEntity(FEED_KEY) }
            val newIds = newEntities.map { it.id }.toSet()
            catalogDao.insertBooks(newEntities)
            catalogDao.deleteStaleBooks(FEED_KEY, newIds.toList())
        }

        result.nextPageUrl
    }.also { if (it.isFailure) Timber.e(it.exceptionOrNull(), "Gutenberg feed sync failed") }

    override suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult> =
        runCatching {
            GutenbergMapper.parseFeed(apiService.getNextPage(nextUrl))
        }

    override suspend fun searchBooks(query: String, page: Int): Result<GutenbergFeedResult> =
        runCatching {
            GutenbergMapper.parseFeed(apiService.searchBooks(query, page))
        }
}