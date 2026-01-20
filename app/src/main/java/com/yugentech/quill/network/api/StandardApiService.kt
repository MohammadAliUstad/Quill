package com.yugentech.quill.network.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class StandardApiService(private val httpClient: HttpClient) {

    companion object {
        private const val BASE_URL = "https://standardebooks.org"
        private const val USER_AGENT = "Quill/UMA21131 (Android; yugentech.kazuki@gmail.com)"
    }

    /**
     * 1. NEW RELEASES (Fast)
     * Fetches the 30 most recently published books.
     * Perfect for the "Hero Banner" or "Just Added" row.
     */
    suspend fun getNewReleases(): String {
        return httpClient.get("$BASE_URL/opds/new-releases") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    /**
     * 2. BY SUBJECT (Genre Rows)
     * Fetches books for a specific category.
     * @param subjectSlug: The URL-friendly name (e.g., "science-fiction", "adventure", "mystery")
     */
    suspend fun getBooksBySubject(subjectSlug: String): String {
        return httpClient.get("$BASE_URL/opds/subjects/$subjectSlug") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    /**
     * 3. SEARCH
     * Finds books matching a query (Title, Author, or Tag).
     */
    suspend fun searchBooks(query: String): String {
        return httpClient.get("$BASE_URL/opds/search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", query)
        }.bodyAsText()
    }

    /**
     * 4. ALL SUBJECTS INDEX
     * Fetches the list of ALL available categories (e.g., "Adventure", "Philosophy").
     * Use this if you want to build a "Browse by Category" screen dynamically.
     */
    suspend fun getSubjectList(): String {
        return httpClient.get("$BASE_URL/opds/subjects") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    /**
     * 5. ALL BOOKS (Legacy/Slow)
     * Fetches the entire catalog (1000+ books).
     * Warning: This is heavy on memory and bandwidth. Use sparingly.
     */
    suspend fun getAllBooks(): String {
        return httpClient.get("$BASE_URL/opds/all") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    suspend fun fetchBookDetails(query: String): String {
        return httpClient.get("$BASE_URL/opds/search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", query)
        }.bodyAsText()
    }
}