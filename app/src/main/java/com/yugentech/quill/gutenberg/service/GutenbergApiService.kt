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

    suspend fun getPopularBooks(page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    suspend fun searchBooks(query: String, page: Int = 1): String {
        return httpClient.get("$BASE_URL/books") {
            parameter("search", query)
            parameter("languages", "en")
            parameter("mime_type", "application/epub")
            parameter("page", page)
        }.bodyAsText()
    }

    suspend fun getNextPage(nextUrl: String): String {
        return httpClient.get(nextUrl).bodyAsText()
    }
}