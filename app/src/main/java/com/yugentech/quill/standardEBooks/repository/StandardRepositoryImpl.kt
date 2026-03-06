package com.yugentech.quill.standardEBooks.repository

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.dao.CategoryCacheDao
import com.yugentech.quill.database.entity.CategoryCacheEntity
import com.yugentech.quill.database.mapper.toCatalogEntity
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.standardEBooks.mapper.StandardEbooksMapper
import com.yugentech.quill.standardEBooks.model.OpdsCollection
import com.yugentech.quill.standardEBooks.model.OpdsFeedResult
import com.yugentech.quill.standardEBooks.service.StandardApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private const val SOURCE_KEY = "standard"

class StandardRepositoryImpl(
    private val standardApi: StandardApiService,
    private val catalogDao: CatalogDao,
    private val categoryCacheDao: CategoryCacheDao,
) : StandardRepository {

    // ── Books ─────────────────────────────────────────────────────────────────

    override fun getNewReleasesFlow(): Flow<List<Book>> {
        return catalogDao.getBooksByCategory("new-releases").map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun syncNewReleases(): Result<Unit> {
        return try {
            val xml = standardApi.getNewReleases()
            val result = StandardEbooksMapper.parseOpdsToBooks(xml)
            val books = result.books

            if (books.isNotEmpty()) {
                // Insert first with REPLACE strategy, then clear stale entries
                // This prevents the Flow from emitting an empty list between clear and insert
                val newEntities = books.map { it.toCatalogEntity("new-releases") }
                val newIds = newEntities.map { it.id }.toSet()
                catalogDao.insertBooks(newEntities)
                catalogDao.deleteStaleBooks("new-releases", newIds.toList())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Background sync failed. UI will just continue showing cached data.")
            Result.failure(e)
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    override fun getCategoriesFlow(): Flow<List<String>> {
        return categoryCacheDao.getCategoriesBySource(SOURCE_KEY)
    }

    override suspend fun syncCategories(): Result<Unit> {
        return try {
            val xml = standardApi.getCategories()
            val categories = StandardEbooksMapper.parseOpdsToCategories(xml)

            if (categories.isNotEmpty()) {
                categoryCacheDao.clearCategories(SOURCE_KEY)
                categoryCacheDao.insertCategories(
                    categories.map { CategoryCacheEntity(name = it, source = SOURCE_KEY) }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Category sync failed. UI will show cached categories.")
            Result.failure(e)
        }
    }

    // ── Search & Discovery ────────────────────────────────────────────────────

    override suspend fun searchBooks(query: String): Result<OpdsFeedResult> {
        return try {
            Result.success(StandardEbooksMapper.parseOpdsToBooks(standardApi.searchBooks(query)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNextPage(nextUrl: String): Result<OpdsFeedResult> {
        return try {
            Result.success(StandardEbooksMapper.parseOpdsToBooks(standardApi.getNextPage(nextUrl)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCollections(): Result<List<OpdsCollection>> {
        return try {
            Result.success(StandardEbooksMapper.parseOpdsToCollections(standardApi.getCollections()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBooksByAuthor(authorName: String): Result<OpdsFeedResult> {
        return try {
            Result.success(StandardEbooksMapper.parseOpdsToBooks(standardApi.getBooksByAuthor(authorName)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}