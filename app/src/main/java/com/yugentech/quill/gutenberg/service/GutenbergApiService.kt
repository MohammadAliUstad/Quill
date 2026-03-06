package com.yugentech.quill.gutenberg.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

class GutenbergApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://gutendex.com"
    }

    // Most popular books (default sort) — used as "Top Books" feed
    suspend fun getPopularBooks(page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    // Books sorted by ID descending — highest IDs are most recently added
    suspend fun getNewReleases(page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("sort", "ascending") // ascending ID = oldest; we'll reverse or use descending
            parameter("page", page)
        }.bodyAsText()
    }

    // Full-text search across titles and author names
    suspend fun searchBooks(query: String, page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("search", query)
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    // Books filtered by subject/topic string (e.g. "Fiction", "Science")
    suspend fun getBooksByTopic(topic: String, page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("topic", topic)
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    // Books by a specific author name
    suspend fun getBooksByAuthor(authorName: String, page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("search", authorName)
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    // Fetch a specific page from a full "next" URL returned by the API
    suspend fun getNextPage(nextUrl: String): String {
        return httpClient.get(nextUrl).bodyAsText()
    }

    // Single book detail by Gutenberg numeric ID
    suspend fun getBookById(id: Int): String {
        return httpClient.get("$BASE_URL/books/$id").bodyAsText()
    }
}