package com.yugentech.quill.sources.gutenberg.mapper

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.sources.gutenberg.model.GutenbergFeedResult
import org.json.JSONObject

object GutenbergMapper {

    fun parseFeed(json: String): GutenbergFeedResult {
        val books = mutableListOf<Book>()
        var nextPageUrl: String? = null

        try {
            val root = JSONObject(json)
            nextPageUrl = root.optString("next").takeIf { it.isNotBlank() && it != "null" }

            val results = root.optJSONArray("results")
                ?: return GutenbergFeedResult(
                    emptyList(),
                    null
                )

            for (i in 0 until results.length()) {
                val entry = results.optJSONObject(i) ?: continue
                mapToBook(entry)?.let { books.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return GutenbergFeedResult(
            books,
            nextPageUrl
        )
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