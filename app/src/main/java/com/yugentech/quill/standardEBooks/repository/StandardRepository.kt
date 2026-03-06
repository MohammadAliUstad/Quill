package com.yugentech.quill.standardEBooks.repository

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.standardEBooks.model.OpdsCollection
import com.yugentech.quill.standardEBooks.model.OpdsFeedResult
import kotlinx.coroutines.flow.Flow

interface StandardRepository {
    fun getNewReleasesFlow(): Flow<List<Book>>
    fun getCategoriesFlow(): Flow<List<String>>
    suspend fun syncNewReleases(): Result<Unit>
    suspend fun syncCategories(): Result<Unit>
    suspend fun searchBooks(query: String): Result<OpdsFeedResult>
    suspend fun getNextPage(nextUrl: String): Result<OpdsFeedResult>
    suspend fun getCollections(): Result<List<OpdsCollection>>
    suspend fun getBooksByAuthor(authorName: String): Result<OpdsFeedResult>
}