package com.yugentech.quill.gutenberg.mapper

import com.yugentech.quill.gutenberg.model.GutenbergCategory
import com.yugentech.quill.gutenberg.model.GutenbergFeedResult
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import org.json.JSONObject

object GutenbergMapper {

    fun parseFeed(json: String): GutenbergFeedResult {
        val books = mutableListOf<Book>()
        var nextPageUrl: String? = null
        var totalCount = 0

        try {
            val root = JSONObject(json)
            totalCount = root.optInt("count", 0)
            nextPageUrl = root.optString("next").takeIf { it.isNotBlank() && it != "null" }

            val results = root.optJSONArray("results")
                ?: return GutenbergFeedResult(emptyList(), null, 0)

            for (i in 0 until results.length()) {
                val entry = results.optJSONObject(i) ?: continue
                mapToBook(entry)?.let { books.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return GutenbergFeedResult(books, nextPageUrl, totalCount)
    }

    fun parseSingleBook(json: String): Book? {
        return try {
            mapToBook(JSONObject(json))
        } catch (e: Exception) {
            null
        }
    }

    fun parseCategoriesFromFeed(json: String): List<GutenbergCategory> {
        val names = mutableSetOf<String>()
        try {
            val root = JSONObject(json)
            val results = root.optJSONArray("results") ?: return emptyList()
            for (i in 0 until results.length()) {
                val entry = results.optJSONObject(i) ?: continue
                val shelves = entry.optJSONArray("bookshelves") ?: continue
                for (j in 0 until shelves.length()) {
                    val raw = shelves.optString(j).trim()
                    val cleaned = raw
                        .removePrefix("Category: ")
                        .removePrefix("category: ")
                        .trim()
                    if (cleaned.isValidCategory()) names.add(cleaned)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return names.sorted().map { GutenbergCategory(it) }
    }

    private fun String.isValidCategory(): Boolean {
        if (isBlank() || length > 30) return false
        if (contains("list", ignoreCase = true)) return false
        if (contains("'s")) return false
        if (contains(" from ", ignoreCase = true)) return false
        if (contains(" by ", ignoreCase = true)) return false
        if (any { it.isDigit() }) return false
        return true
    }

    private fun mapToBook(entry: JSONObject): Book? {
        val id = entry.optInt("id", -1).takeIf { it > 0 } ?: return null
        val rawTitle = entry.optString("title").takeIf { it.isNotBlank() } ?: return null
        val title = cleanTitle(rawTitle)

        val authorsArray = entry.optJSONArray("authors")
        val author = buildString {
            if (authorsArray != null) {
                for (i in 0 until authorsArray.length()) {
                    val a = authorsArray.optJSONObject(i) ?: continue
                    val raw = a.optString("name", "")
                    if (raw.isNotBlank()) {
                        if (isNotEmpty()) append(" & ")
                        append(formatAuthorName(raw))
                    }
                }
            }
        }.ifBlank { "Unknown Author" }

        val subjectsArray = entry.optJSONArray("subjects")
        val subjects = mutableListOf<String>()
        if (subjectsArray != null) {
            for (i in 0 until subjectsArray.length()) {
                val s = subjectsArray.optString(i).trim()
                if (s.isNotBlank()) subjects.add(s.substringBefore(" --").trim())
            }
        }

        val formats = entry.optJSONObject("formats")
        val coverUrl = formats?.let {
            it.optString("image/jpeg").takeIf { u -> u.isNotBlank() && u != "null" }
        }
        val downloadUrl = formats?.let {
            it.optString("application/epub+zip").takeIf { u -> u.isNotBlank() && u != "null" }
                ?: it.optString("application/epub").takeIf { u -> u.isNotBlank() && u != "null" }
        } ?: ""

        val summariesArray = entry.optJSONArray("summaries")
        val description = summariesArray?.let {
            if (it.length() > 0) it.optString(0).takeIf { s -> s.isNotBlank() }
            else null
        }

        val language = entry.optJSONArray("languages")?.optString(0) ?: "en"

        return Book(
            id = "gutenberg_$id",
            title = title,
            author = author,
            description = description,
            coverUrl = coverUrl,
            downloadUrl = downloadUrl,
            source = BookSource.GUTENBERG,
            subjects = subjects.distinct(),
            language = language
        )
    }

    private fun cleanTitle(raw: String): String {
        val cleaned = raw
            .substringBefore("; ")
            .substringBefore(" : ")
            .trim()
        val suffixPattern =
            Regex(""",\s*(Complete|Vol\.|Volume|Part|Book)\b.*$""", RegexOption.IGNORE_CASE)
        return suffixPattern.replace(cleaned, "").trim()
    }

    private fun formatAuthorName(raw: String): String {
        val parts = raw.split(",")
        return if (parts.size >= 2) "${parts[1].trim()} ${parts[0].trim()}" else raw.trim()
    }
}