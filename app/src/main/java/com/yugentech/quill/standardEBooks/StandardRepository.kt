package com.yugentech.quill.network

import com.yugentech.quill.network.domain.Book

interface StandardRepository {
    suspend fun getNewReleases(): Result<List<Book>>
    suspend fun searchBooks(query: String): Result<List<Book>>
    suspend fun getBookDetails(query: String): Result<Book?>
    suspend fun getBookById(id: String): Book?
}