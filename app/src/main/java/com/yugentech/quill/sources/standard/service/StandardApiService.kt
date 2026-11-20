package com.yugentech.quill.sources.standard.service

import io.ktor.client.plugins.timeout
import com.yugentech.quill.sources.shared.throwIfMaintenance
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import com.yugentech.quill.sources.shared.bodyUnlessMaintenance

class StandardApiService(
    private val httpClient: HttpClient
) {
    // A 503 means standardebooks.org is down for maintenance: thrown as ServerMaintenanceException
    companion object {
        // Feeds live under /feeds/opds; the old /opds paths only redirect there, costing a round trip.
        private const val BASE_URL = "https://standardebooks.org"
        private const val USER_AGENT = "Quill/UMA21131 (Android; yugentech.kazuki@gmail.com)"
    }

    /**
     * Whether standardebooks.org is up, by asking it for its smallest file. Throws
     * [com.yugentech.quill.sources.shared.ServerMaintenanceException] if it's down for maintenance.
     */
    suspend fun checkSite() {
        httpClient.get("$BASE_URL/robots.txt") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            timeout { requestTimeoutMillis = 15_000 }
        }.throwIfMaintenance()
    }

    suspend fun getNewReleases(): String {
        return httpClient.get("$BASE_URL/feeds/opds/new-releases") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyUnlessMaintenance()
    }

    suspend fun searchBooks(query: String): String {
        return httpClient.get("$BASE_URL/feeds/opds/all") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", query)
        }.bodyUnlessMaintenance()
    }

    suspend fun getCategories(): String {
        return httpClient.get("$BASE_URL/feeds/opds/subjects") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyUnlessMaintenance()
    }

    suspend fun getTopicFeed(topic: String): String {
        return httpClient.get("$BASE_URL/feeds/opds/subjects/$topic") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyUnlessMaintenance()
    }

    suspend fun getNextPage(nextUrl: String): String {
        val url = when {
            nextUrl.startsWith("http") -> nextUrl
            nextUrl.startsWith("/") -> "$BASE_URL$nextUrl"
            else -> "$BASE_URL/$nextUrl"
        }
        return httpClient.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyUnlessMaintenance()
    }

    suspend fun getCollections(): String {
        return httpClient.get("$BASE_URL/feeds/opds/collections") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyUnlessMaintenance()
    }

    suspend fun getBooksByAuthor(authorName: String): String {
        return httpClient.get("$BASE_URL/feeds/opds/all") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", "author:\"$authorName\"")
        }.bodyUnlessMaintenance()
    }
}