package com.yugentech.quill.standardEBooks

import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.network.domain.BookSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

// --- NEW DATA WRAPPERS ---
data class OpdsFeedResult(
    val books: List<Book>,
    val nextPageUrl: String?
)

data class OpdsCollection(
    val title: String,
    val feedUrl: String
)

object StandardEbooksMapper {

    // UPDATED: Now returns OpdsFeedResult so we can capture the pagination link
    fun parseOpdsToBooks(xmlString: String): OpdsFeedResult {
        val books = mutableListOf<Book>()
        var nextPageUrl: String? = null

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))

            var eventType = parser.eventType
            var currentBook: MutableMap<String, String>? = null
            var currentTag: String? = null
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        currentText.clear()

                        if (currentTag.equals("entry", ignoreCase = true)) {
                            currentBook = mutableMapOf()
                        }

                        if (currentTag.equals("link", ignoreCase = true)) {
                            val rel = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href")
                            val type = parser.getAttributeValue(null, "type")

                            if (rel != null && href != null) {
                                // NEW: Intercept root-level "next" pagination link
                                if (currentBook == null && rel.equals("next", ignoreCase = true)) {
                                    nextPageUrl = href
                                }
                                // Existing logic for book-level links
                                else if (currentBook != null) {
                                    when {
                                        rel.contains("image") || rel.contains("thumbnail") -> {
                                            currentBook["coverUrl"] = href
                                        }
                                        rel.contains("acquisition") && type?.contains("epub") == true -> {
                                            currentBook["downloadUrl"] = href
                                        }
                                    }
                                }
                            }
                        }

                        if (currentTag.equals("category", ignoreCase = true) && currentBook != null) {
                            val term = parser.getAttributeValue(null, "term")
                            val label = parser.getAttributeValue(null, "label")
                            val subject = label ?: term
                            if (subject != null) {
                                val existing = currentBook["subjects"] ?: ""
                                currentBook["subjects"] = if (existing.isEmpty()) subject else "$existing,$subject"
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        currentText.append(parser.text)
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name

                        if (currentBook != null) {
                            val text = currentText.toString().trim()

                            when {
                                tagName.equals("id", ignoreCase = true) -> currentBook["id"] = text
                                tagName.equals("title", ignoreCase = true) -> currentBook["title"] = text
                                tagName.equals("name", ignoreCase = true) -> currentBook["author"] = text
                                tagName.equals("summary", ignoreCase = true) || tagName.equals("content", ignoreCase = true) -> {
                                    if (text.isNotEmpty()) currentBook["description"] = text
                                }
                                tagName.equals("entry", ignoreCase = true) -> {
                                    mapToBook(currentBook)?.let { books.add(it) }
                                    currentBook = null
                                }
                            }
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return both the mapped books AND the link to the next page
        return OpdsFeedResult(books, nextPageUrl)
    }

    fun parseOpdsToCategories(xmlString: String): List<String> {
        val categories = mutableListOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))

            var eventType = parser.eventType
            var insideEntry = false
            var currentTag: String? = null
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        currentText.clear()

                        if (currentTag.equals("entry", ignoreCase = true)) {
                            insideEntry = true
                        }
                    }

                    XmlPullParser.TEXT -> {
                        currentText.append(parser.text)
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name

                        if (insideEntry && tagName.equals("title", ignoreCase = true)) {
                            val categoryName = currentText.toString().trim()
                            if (categoryName.isNotEmpty()) {
                                categories.add(categoryName)
                            }
                        }

                        if (tagName.equals("entry", ignoreCase = true)) {
                            insideEntry = false
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return categories.distinct()
    }

    // --- NEW FUNCTION: Parses Collections ---
    fun parseOpdsToCollections(xmlString: String): List<OpdsCollection> {
        val collections = mutableListOf<OpdsCollection>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString))

            var eventType = parser.eventType
            var insideEntry = false
            var currentTag: String? = null
            val currentText = StringBuilder()

            var currentTitle = ""
            var currentUrl = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        currentText.clear()

                        if (currentTag.equals("entry", ignoreCase = true)) {
                            insideEntry = true
                            currentTitle = ""
                            currentUrl = ""
                        }

                        // Capture the feed URL specifically for this collection
                        if (insideEntry && currentTag.equals("link", ignoreCase = true)) {
                            val rel = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href")
                            if (rel != null && rel.contains("subsection") && href != null) {
                                currentUrl = href
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        currentText.append(parser.text)
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name

                        if (insideEntry && tagName.equals("title", ignoreCase = true)) {
                            currentTitle = currentText.toString().trim()
                        }

                        if (tagName.equals("entry", ignoreCase = true)) {
                            if (currentTitle.isNotEmpty() && currentUrl.isNotEmpty()) {
                                collections.add(OpdsCollection(currentTitle, currentUrl))
                            }
                            insideEntry = false
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return collections
    }

    // --- PRIVATE METHODS ---
    private fun mapToBook(data: Map<String, String>): Book? {
        val title = data["title"] ?: return null
        val idRaw = data["id"] ?: return null

        val cleanId = "standardebooks_" + idRaw.trim()
            .removeSuffix("/")
            .substringAfter("ebooks/")
            .replace("/", "_")

        return Book(
            id = cleanId,
            title = title,
            author = data["author"] ?: "Unknown Author",
            description = data["description"],
            coverUrl = resolveCoverUrl(data["coverUrl"]),
            downloadUrl = data["downloadUrl"] ?: "",
            source = BookSource.STANDARD_EBOOKS,
            subjects = data["subjects"]?.split(",") ?: emptyList(),
            language = "en"
        )
    }

    private fun resolveCoverUrl(url: String?): String? {
        if (url == null) return null
        return if (url.startsWith("http")) url else "https://standardebooks.org$url"
    }
}