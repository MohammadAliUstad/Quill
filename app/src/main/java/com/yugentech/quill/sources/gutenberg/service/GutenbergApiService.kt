package com.yugentech.quill.sources.gutenberg.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.client.plugins.ResponseException
import com.yugentech.quill.sources.shared.ServerMaintenanceException
import com.yugentech.quill.sources.shared.throwIfMaintenance
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
        return response.bodyOrThrow()
    }

    /**
     * Whether gutenberg.org itself is up, for the Sources card and downloads. The popular grid
     * comes from Gutendex, which stays up while gutenberg.org is down for maintenance, but its
     * covers and downloads are all on gutenberg.org. Throws [ServerMaintenanceException] if it's down.
     */
    suspend fun checkSite() {
        httpClient.get("$SITE/robots.txt") {
            timeout {
                requestTimeoutMillis = 15_000
            }
        }.throwIfMaintenance()
    }

    /**
     * The body of a successful response. Anything else (gutenberg.org answers 503 with an HTML
     * page while it's down for maintenance) is thrown, so it shows as a failed load rather than
     * being parsed into an empty "no results" list.
     */
    private suspend fun HttpResponse.bodyOrThrow(): String {
        if (!status.isSuccess()) throw ResponseException(this, "${request.url.host} answered $status")
        return bodyAsText()
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
        }.bodyOrThrow()
    }

    /** One book's own feed on gutenberg.org: its summary and subjects, for the details page. */
    suspend fun getBookFeed(number: Int): String {
        return httpClient.get("$SITE/ebooks/$number.opds") {
            timeout {
                requestTimeoutMillis = 30_000
            }
        }.bodyOrThrow()
    }

    suspend fun getNextPage(nextUrl: String): String {
        return httpClient.get(nextUrl) {
            timeout {
                requestTimeoutMillis = 60_000
            }
        }.bodyOrThrow()
    }
}