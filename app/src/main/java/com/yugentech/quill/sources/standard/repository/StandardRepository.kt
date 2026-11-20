package com.yugentech.quill.sources.standard.repository

import com.yugentech.quill.sources.shared.ServerMaintenanceException
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.standard.model.OpdsCollection
import com.yugentech.quill.sources.standard.model.OpdsFeedResult
import kotlinx.coroutines.flow.Flow

interface StandardRepository {
    /**
     * Asks standardebooks.org whether it's down: its maintenance if so, or null if it answered
     * and is up. A failure means it couldn't be asked (offline, a timeout), which says neither.
     */
    suspend fun checkMaintenance(): Result<ServerMaintenanceException?>
    fun getNewReleasesFlow(): Flow<List<Book>>
    fun getCategoriesFlow(): Flow<List<String>>
    fun getTopicBooksFlow(topic: String): Flow<List<Book>>
    suspend fun syncNewReleases(): Result<Unit>
    suspend fun syncCategories(): Result<Unit>
    suspend fun syncTopicBooks(topic: String): Result<Unit>
    suspend fun searchBooks(query: String): Result<OpdsFeedResult>
    suspend fun getCollections(): Result<List<OpdsCollection>>
    suspend fun getBooksByAuthor(authorName: String): Result<OpdsFeedResult>
}