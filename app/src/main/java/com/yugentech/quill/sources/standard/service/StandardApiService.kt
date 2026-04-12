package com.yugentech.quill.sources.standard.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

class StandardApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://standardebooks.org"
        private const val USER_AGENT = "Quill/UMA21131 (Android; yugentech.kazuki@gmail.com)"
    }

    suspend fun getNewReleases(): String {
        return httpClient.get("$BASE_URL/opds/new-releases") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    suspend fun searchBooks(query: String): String {
        return httpClient.get("$BASE_URL/opds/search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", query)
        }.bodyAsText()
    }

    suspend fun getCategories(): String {
        return httpClient.get("$BASE_URL/opds/subjects") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
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
        }.bodyAsText()
    }

    suspend fun getCollections(): String {
        return httpClient.get("$BASE_URL/opds/collections") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
        }.bodyAsText()
    }

    suspend fun getBooksByAuthor(authorName: String): String {
        return httpClient.get("$BASE_URL/opds/search") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/atom+xml")
            parameter("query", "author:\"$authorName\"")
        }.bodyAsText()
    }
}