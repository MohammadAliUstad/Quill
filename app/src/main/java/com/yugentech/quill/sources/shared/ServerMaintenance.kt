package com.yugentech.quill.sources.shared

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A source's server is down for maintenance: it answered 503 Service Unavailable. [backAfter] is
 * when it said it would return, if its maintenance page gave a time we could read.
 */
class ServerMaintenanceException(
    val host: String,
    val backAfter: Instant?
) : IOException("$host is down for maintenance")

/** Throws [ServerMaintenanceException] when the server answered that it's down for maintenance. */
suspend fun HttpResponse.throwIfMaintenance() {
    if (status == HttpStatusCode.ServiceUnavailable) {
        val page = runCatching { bodyAsText() }.getOrNull().orEmpty()
        throw ServerMaintenanceException(request.url.host, parseBackAfter(page))
    }
}

/** The body, unless the server is down for maintenance; any other answer is passed on as before. */
suspend fun HttpResponse.bodyUnlessMaintenance(): String {
    throwIfMaintenance()
    return bodyAsText()
}

// Project Gutenberg's page: "Please check back after 21:00 UTC on Wednesday, 18 November 2026."
private val BackAfterPattern = Regex(
    """after\s+(\d{1,2}:\d{2})\s+UTC\s+on\s+(?:[A-Za-z]+,\s+)?(\d{1,2}\s+[A-Za-z]+\s+\d{4})""",
    RegexOption.IGNORE_CASE
)
private val BackAfterFormat = DateTimeFormatter.ofPattern("H:mm d MMMM yyyy", Locale.ENGLISH)

/** When a maintenance page says the server will be back, or null if it doesn't say (readably). */
internal fun parseBackAfter(page: String): Instant? {
    val match = BackAfterPattern.find(page) ?: return null
    val (time, date) = match.destructured
    return runCatching {
        LocalDateTime.parse("$time $date", BackAfterFormat).toInstant(ZoneOffset.UTC)
    }.getOrNull()
}
