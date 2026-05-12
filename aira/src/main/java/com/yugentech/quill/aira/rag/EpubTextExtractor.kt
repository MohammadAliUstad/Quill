package com.yugentech.quill.aira.rag

import android.content.Context
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber
import java.io.File

data class ChapterText(
    val chapterIndex: Int,
    val chapterTitle: String,
    val text: String
)

class EpubTextExtractor(
    private val context: Context
) {

    companion object {
        private const val MIN_CHAPTER_LENGTH = 50
        private val SCRIPT_REGEX =
            Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
        private val STYLE_REGEX = Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
        private val WHITESPACE_REGEX = Regex("[ \\t]+")
        private val NEWLINE_REGEX = Regex("\\n{3,}")

        private val IGNORED_TITLES = setOf(
            "titlepage", "imprint", "colophon", "uncopyright",
            "dedication", "epigraph", "endnotes", "bibliography",
            "index", "list of illustrations", "cover", "table of contents",
            "preface", "acknowledgments", "introduction", "loi"
        )
    }

    fun extractStream(filePath: String): Flow<ChapterText> = flow {
        val file = File(filePath)
        if (!file.exists()) {
            Timber.e("✗ File not found at path: $filePath")
            return@flow
        }

        try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
            val publicationOpener = PublicationOpener(parser, emptyList(), onCreatePublication = {})

            val asset = assetRetriever.retrieve(file).getOrElse { err ->
                Timber.e("✗ Failed to retrieve asset: $err")
                return@flow
            }

            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse { err ->
                    Timber.e("✗ Failed to open publication: $err")
                    return@flow
                }

            val allTocLinks = flattenLinks(publication.tableOfContents)
            val hrefToTitleMap = allTocLinks.mapNotNull { link ->
                val title = link.title?.trim()
                if (title.isNullOrBlank()) {
                    null
                } else {
                    link.href.toString().substringBefore("#") to title
                }
            }.toMap()

            var validChapterCount = 0

            publication.readingOrder.forEachIndexed { index, link ->
                val cleanHref = link.href.toString().substringBefore("#")
                val rawTitle = hrefToTitleMap[cleanHref] ?: link.title?.trim() ?: ""
                val lowerTitle = rawTitle.lowercase()
                val isJunk = IGNORED_TITLES.contains(lowerTitle) || lowerTitle.startsWith("spine[")

                if (isJunk) {
                    return@forEachIndexed
                }

                validChapterCount++
                val finalTitle = rawTitle.ifBlank { "Chapter $validChapterCount" }

                try {
                    val resource = publication.get(link) ?: return@forEachIndexed

                    val bytes = resource.read().getOrElse {
                        return@forEachIndexed
                    }

                    val plainText = stripHtml(String(bytes, Charsets.UTF_8))

                    if (plainText.length >= MIN_CHAPTER_LENGTH) {
                        emit(
                            ChapterText(
                                chapterIndex = index,
                                chapterTitle = finalTitle,
                                text = plainText
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error parsing chapter $index ('$finalTitle')")
                }
            }

            publication.close()

        } catch (e: Exception) {
            Timber.e(e, "✗ Fatal error during EPUB extraction stream")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun countChapters(filePath: String): Int = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext 0
        try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
            val publicationOpener = PublicationOpener(parser, emptyList(), onCreatePublication = {})
            val asset = assetRetriever.retrieve(file).getOrElse { return@withContext 0 }
            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse { return@withContext 0 }
            val count = publication.readingOrder.count { link ->
                true
            }
            publication.close()
            count
        } catch (e: Exception) {
            Timber.w(e, "Failed to count chapters for $filePath")
            0
        }
    }

    private fun stripHtml(html: String): String =
        HtmlCompat.fromHtml(
            html.replace(SCRIPT_REGEX, " ").replace(STYLE_REGEX, " "),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        ).toString()
            .replace('\u00A0', ' ')
            .replace(WHITESPACE_REGEX, " ")
            .replace(NEWLINE_REGEX, "\n\n")
            .trim()

    private fun flattenLinks(links: List<Link>): List<Link> {
        val flatList = mutableListOf<Link>()
        for (link in links) {
            flatList.add(link)
            if (link.children.isNotEmpty()) {
                flatList.addAll(flattenLinks(link.children))
            }
        }
        return flatList
    }
}