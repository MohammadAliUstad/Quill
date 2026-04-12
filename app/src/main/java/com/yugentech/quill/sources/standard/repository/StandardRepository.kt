package com.yugentech.quill.sources.standard.repository

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.standard.model.OpdsCollection
import com.yugentech.quill.sources.standard.model.OpdsFeedResult
import kotlinx.coroutines.flow.Flow

interface StandardRepository {
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