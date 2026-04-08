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

    override fun getTopicBooksFlow(topic: String): Flow<List<Book>> {
        val feedKey = "standard-topic-$topic"
        return catalogDao.getBooksByCategory(feedKey).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun syncTopicBooks(topic: String): Result<Unit> {
        return try {
            val xml = standardApi.searchBooks("subject:\"$topic\"")
            val result = StandardEbooksMapper.parseOpdsToBooks(xml)

            if (result.books.isNotEmpty()) {
                val feedKey = "standard-topic-$topic"
                val newEntities = result.books.map { it.toCatalogEntity(feedKey) }
                val newIds = newEntities.map { it.id }.toSet()

                catalogDao.insertBooks(newEntities)
                catalogDao.deleteStaleBooks(feedKey, newIds.toList())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Topic sync failed for: $topic")
            Result.failure(e)
        }
    }

    override suspend fun searchBooks(query: String): Result<OpdsFeedResult> {
        return try {
            Result.success(StandardEbooksMapper.parseOpdsToBooks(standardApi.searchBooks(query)))
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