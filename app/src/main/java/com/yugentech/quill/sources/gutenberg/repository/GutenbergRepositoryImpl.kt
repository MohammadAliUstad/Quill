package com.yugentech.quill.sources.gutenberg.repository

import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.mapper.toCatalogEntity
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.mapper.GutenbergMapper
import com.yugentech.quill.sources.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.sources.gutenberg.service.GutenbergApiService
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

    override suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult> = try {
        Result.success(GutenbergMapper.parseFeed(apiService.getNextPage(nextUrl)))
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun searchBooks(query: String, page: Int): Result<GutenbergFeedResult> = try {
        Result.success(GutenbergMapper.parseFeed(apiService.searchBooks(query, page)))
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(e)
    }
}