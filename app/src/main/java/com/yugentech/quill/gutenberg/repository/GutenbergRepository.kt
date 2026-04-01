package com.yugentech.quill.gutenberg.repository

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.gutenberg.model.GutenbergFeedResult
import kotlinx.coroutines.flow.Flow

interface GutenbergRepository {
    fun getPopularBooksFlow(): Flow<List<Book>>
    suspend fun searchBooks(query: String, page: Int = 1): Result<GutenbergFeedResult>
    suspend fun getNextPage(nextUrl: String): Result<GutenbergFeedResult>
    suspend fun syncPopularFeed(): Result<String?>
}