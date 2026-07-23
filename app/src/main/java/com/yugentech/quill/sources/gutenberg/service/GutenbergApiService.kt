package com.yugentech.quill.sources.gutenberg.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

class GutenbergApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://gutendex.com"
    }

    suspend fun getPopularBooks(page: Int = 1): String {
        Timber.d("getPopularBooks() hitting URL: $BASE_URL/books/?languages=en&page=$page")
        val response = httpClient.get("$BASE_URL/books/") {
            timeout {
                requestTimeoutMillis = 60_000
            }
            parameter("languages", "en")
            parameter("page", page)
        }
        Timber.d("getPopularBooks() response status=${response.status}")
        return response.bodyAsText()
    }

    suspend fun searchBooks(query: String, page: Int = 1): String {
        return httpClient.get("$BASE_URL/books/") {
            timeout {
                requestTimeoutMillis = 60_000
            }
            parameter("search", query)
            parameter("languages", "en")
            parameter("page", page)
        }.bodyAsText()
    }

    suspend fun getNextPage(nextUrl: String): String {
        return httpClient.get(nextUrl) {
            timeout {
                requestTimeoutMillis = 60_000
            }
        }.bodyAsText()
    }
}