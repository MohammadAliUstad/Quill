package com.yugentech.quill.sources.gutenberg.mapper

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.sources.gutenberg.model.GutenbergFeedResult
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Reads Project Gutenberg's own OPDS feeds (gutenberg.org), which answer in a second or two where
 * Gutendex's search can take a minute.
 *
 * - Search results ([parseSearchFeed]) carry each book's number, title and author. The cover and
 *   EPUB links follow fixed patterns from the number (the same links Gutendex returns), so a
 *   result is a complete [Book] apart from its summary and subjects.
 * - A book's own feed ([parseBookFeed]) adds those: the summary and subject headings, for the
 *   details page.
 */
object GutenbergOpdsMapper {

    const val SITE = "https://www.gutenberg.org"

    private val BOOK_FEED_ID = Regex("""/ebooks/(\d+)\.opds$""")

    fun coverUrl(number: Int) = "$SITE/cache/epub/$number/pg$number.cover.medium.jpg"
    fun downloadUrl(number: Int) = "$SITE/ebooks/$number.epub3.images"

    /** Our id for a Gutenberg book ("gutenberg_2600"), as GutenbergMapper builds it. */
    fun bookId(number: Int) = "gutenberg_$number"

    /** The Gutenberg number back from our id, or null if it isn't a Gutenberg book. */
    fun numberOf(bookId: String): Int? = bookId.removePrefix("gutenberg_").toIntOrNull()
        ?.takeIf { bookId.startsWith("gutenberg_") }

    /**
     * Book entries of a search feed. Entries that aren't books (the "Subjects" and "Authors"
     * shortcuts Gutenberg mixes in) have no book number in their id and are skipped.
     */
    fun parseSearchFeed(xml: String): GutenbergFeedResult {
        val books = mutableListOf<Book>()
        var nextPageUrl: String? = null

        try {
            val parser = newParser(xml)
            var inEntry = false
            var currentTag: String? = null
            var entryId = ""
            var title = ""
            var content = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (parser.name) {
                            "entry" -> {
                                inEntry = true
                                entryId = ""; title = ""; content = ""
                            }
                            "link" -> if (!inEntry && parser.getAttributeValue(null, "rel") == "next") {
                                nextPageUrl = parser.getAttributeValue(null, "href")?.let(::absolute)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> if (inEntry) {
                        val text = parser.text
                        when (currentTag) {
                            "id" -> entryId += text
                            "title" -> title += text
                            "content" -> content += text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "entry") {
                            inEntry = false
                            val number = BOOK_FEED_ID.find(entryId.trim())?.groupValues?.get(1)?.toIntOrNull()
                            if (number != null && title.isNotBlank()) {
                                books += Book(
                                    id = bookId(number),
                                    title = GutenbergMapper.cleanTitle(title.trim()),
                                    // Search entries give the author already in reading order.
                                    author = content.trim().ifBlank { "Unknown Author" },
                                    description = null,
                                    coverUrl = coverUrl(number),
                                    downloadUrl = downloadUrl(number),
                                    source = BookSource.GUTENBERG,
                                    subjects = emptyList(),
                                    language = "en"
                                )
                            }
                        }
                        currentTag = null
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return GutenbergFeedResult(books.distinctBy { it.id }, nextPageUrl)
    }

    /** What a book's own feed adds to a search result: its summary and subject headings. */
    data class BookExtras(val summary: String?, val subjects: List<String>)

    fun parseBookFeed(xml: String): BookExtras {
        var summary: String? = null
        val subjects = mutableListOf<String>()

        try {
            val parser = newParser(xml)
            var inEntry = false
            var inContent = false
            val contentText = StringBuilder()

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "entry" -> inEntry = true
                        "content" -> if (inEntry) inContent = true
                        // Library of Congress subject headings, e.g. "Love stories".
                        "category" -> if (inEntry && parser.getAttributeValue(null, "scheme")?.endsWith("/LCSH") == true) {
                            parser.getAttributeValue(null, "term")
                                ?.substringBefore(" --")?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let { subjects += it }
                        }
                        // Paragraphs and line breaks inside the description keep their gaps.
                        "p", "br" -> if (inContent) contentText.append('\n')
                    }
                    XmlPullParser.TEXT -> if (inContent) contentText.append(parser.text)
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "content" -> if (inContent) {
                            inContent = false
                            summary = summaryFrom(contentText.toString())
                        }
                        // Only the first entry is the book itself.
                        "entry" -> if (inEntry) {
                            inEntry = false
                            return BookExtras(summary, subjects.distinct())
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return BookExtras(summary, subjects.distinct())
    }

    /** The "Summary:" paragraph of a book's description, without its label. */
    private fun summaryFrom(description: String): String? {
        val paragraphs = description.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        val start = paragraphs.indexOfFirst { it.startsWith("Summary:") }
        if (start < 0) return null
        val inline = paragraphs[start].removePrefix("Summary:").trim()
        // The label sits on its own line with the text on the next one.
        val text = inline.ifEmpty { paragraphs.getOrNull(start + 1).orEmpty() }
        return text.takeIf { it.isNotBlank() }
    }

    private fun absolute(href: String) = if (href.startsWith("http")) href else "$SITE$href"

    private fun newParser(xml: String): XmlPullParser =
        XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader(xml))
        }
}
