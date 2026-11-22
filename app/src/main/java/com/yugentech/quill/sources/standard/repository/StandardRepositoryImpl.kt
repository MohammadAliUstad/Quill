package com.yugentech.quill.sources.standard.repository

import com.yugentech.quill.sources.shared.ServerMaintenanceException
import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.dao.CategoryCacheDao
import com.yugentech.quill.database.entity.CatalogCacheEntity
import com.yugentech.quill.database.entity.CategoryCacheEntity
import com.yugentech.quill.database.mapper.toCatalogEntities
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.shared.CatalogMemory
import com.yugentech.quill.sources.standard.mapper.StandardMapper
import com.yugentech.quill.sources.standard.model.OpdsCollection
import com.yugentech.quill.sources.standard.model.OpdsFeedResult
import com.yugentech.quill.sources.standard.service.StandardApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

private const val SOURCE_KEY = "standard"
private const val NEW_RELEASES_KEY = "new-releases"

private fun topicKey(topic: String) = "standard-topic-$topic"

class StandardRepositoryImpl(
    private val standardApi: StandardApiService,
    private val catalogDao: CatalogDao,
    private val categoryCacheDao: CategoryCacheDao,
) : StandardRepository {

    private val memory = CatalogMemory()

    override suspend fun checkMaintenance(): Result<ServerMaintenanceException?> = try {
        standardApi.checkSite()
        Result.success(null)
    } catch (e: ServerMaintenanceException) {
        Timber.w("Source is down for maintenance")
        Result.success(e)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Timber.w(e, "Source maintenance check failed")
        Result.failure(e)
    }

    override fun getNewReleasesFlow(): Flow<List<Book>> = cachedList(NEW_RELEASES_KEY)

    override suspend fun syncNewReleases(): Result<Unit> {
        return try {
            val xml = standardApi.getNewReleases()
            val result = StandardMapper.parseOpdsToBooks(xml)
            val books = result.books

            if (books.isNotEmpty()) store(NEW_RELEASES_KEY, books)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.w(e, "Background sync failed")
            Result.failure(e)
        }
    }

    /** The subjects last read, so the chip row is there in the first frame along with the books. */
    @Volatile
    private var rememberedCategories: List<String>? = null

    override fun getCategoriesFlow(): Flow<List<String>> = flow {
        rememberedCategories?.let { emit(it) }
        emitAll(categoryCacheDao.getCategoriesBySource(SOURCE_KEY).onEach { if (it.isNotEmpty()) rememberedCategories = it })
    }.distinctUntilChanged()

    override suspend fun syncCategories(): Result<Unit> {
        return try {
            val xml = standardApi.getCategories()
            val categories = StandardMapper.parseOpdsToCategories(xml)

            if (categories.isNotEmpty()) {
                // Stamped in the feed's order (the chips list by it), replaced in one go
                val start = System.currentTimeMillis()
                categoryCacheDao.replaceCategories(
                    SOURCE_KEY,
                    categories.mapIndexed { index, name ->
                        CategoryCacheEntity(name = name, source = SOURCE_KEY, cachedAt = start + index)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Category sync failed")
            Result.failure(e)
        }
    }

    override fun getTopicBooksFlow(topic: String): Flow<List<Book>> = cachedList(topicKey(topic))

    override suspend fun syncTopicBooks(topic: String): Result<Unit> {
        return try {
            val xml = standardApi.getTopicFeed(topic)
            val result = StandardMapper.parseOpdsToBooks(xml)

            if (result.books.isNotEmpty()) store(topicKey(topic), result.books)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.w(e, "Topic sync failed")
            Result.failure(e)
        }
    }

    override suspend fun searchBooks(query: String): Result<OpdsFeedResult> {
        return try {
            Result.success(StandardMapper.parseOpdsToBooks(standardApi.searchBooks(query)))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getCollections(): Result<List<OpdsCollection>> {
        return try {
            Result.success(StandardMapper.parseOpdsToCollections(standardApi.getCollections()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBooksByAuthor(authorName: String): Result<OpdsFeedResult> {
        return try {
            Result.success(
                StandardMapper.parseOpdsToBooks(
                    standardApi.getBooksByAuthor(
                        authorName
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** [key]'s cached books, from memory at once and then from the database. */
    private fun cachedList(key: String): Flow<List<Book>> = memory.follow(
        key,
        catalogDao.getBooksByCategory(key).map { entities ->
            entities.map { it.toDomainModel().copy(id = it.id.removePrefix(rowPrefix(key))) }
        }
    )

    /** Replaces [key]'s cached books with a freshly downloaded [books]. */
    private suspend fun store(key: String, books: List<Book>) {
        catalogDao.replaceCategory(key, books.toCatalogEntities(key).map { it.scopedTo(key) })
        memory.remember(key, books)
    }

    /**
     * The cache keeps one row per ID, and the same book is often in New Arrivals and in one or
     * more subjects. Stored under its plain ID, refreshing one of those lists moved the book's
     * row over to it, taking it out of the others: it was missing from them on the next visit,
     * and popped back in a second later when they refreshed in turn (and took it back). Each
     * list's rows carry the list in their ID instead, so every list keeps its own copy. The
     * prefix comes off again when the rows are read, so the books keep their real IDs.
     *
     * Rows stored before this, under plain IDs, aren't in the next refresh's list of fresh IDs,
     * so that refresh clears them out.
     */
    private fun CatalogCacheEntity.scopedTo(key: String) = copy(id = rowPrefix(key) + id)

    private fun rowPrefix(key: String) = "$key::"
}
