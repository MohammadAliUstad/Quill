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
        // Gutendex for the popular grid (fast, it's cached); gutenberg.org for search and details.
        private const val BASE_URL = "https://gutendex.com"
        private const val SITE = "https://www.gutenberg.org"
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

    /**
     * Search goes to Project Gutenberg's own catalogue: about 1.5s, where Gutendex's search took
     * 40-70s for anything it hadn't cached. "l.en" keeps it to English books, as Gutendex's
     * languages=en did, and leaves out the Subjects/Authors shortcuts.
     */
    suspend fun searchBooks(query: String): String {
        return httpClient.get("$SITE/ebooks/search.opds/") {
            timeout {
                requestTimeoutMillis = 30_000
            }
            parameter("query", "$query l.en")
        }.bodyAsText()
    }

    /** One book's own feed on gutenberg.org: its summary and subjects, for the details page. */
    suspend fun getBookFeed(number: Int): String {
        return httpClient.get("$SITE/ebooks/$number.opds") {
            timeout {
                requestTimeoutMillis = 30_000
            }
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