package com.yugentech.quill.gutenberg.repository

import com.yugentech.quill.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.database.model.Book
import kotlinx.coroutines.flow.Flow

interface GutenbergRepository {
    // Cache-backed flows
    fun getPopularBooksFlow(): Flow<List<Book>>
    fun getCategoriesFlow(): Flow<List<String>>
    fun getTopicBooksFlow(topic: String): Flow<List<Book>>

    // Single sync call — fetches popular books + categories in one network request
    suspend fun syncPopularFeed(): Result<Unit>

    // Cache topic books for instant reload
    suspend fun syncTopicBooks(topic: String): Result<Unit>

    // Network-only
    suspend fun searchBooks(query: String, page: Int = 1): Result<GutenbergFeedResult>
    suspend fun getBooksByTopic(topic: String, page: Int = 1): Result<GutenbergFeedResult>
    suspend fun getBooksByAuthor(authorName: String, page: Int = 1): Result<GutenbergFeedResult>
    suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult>
    suspend fun getBookById(id: Int): Result<Book>
}