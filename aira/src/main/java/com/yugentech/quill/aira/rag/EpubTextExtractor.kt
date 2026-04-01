package com.yugentech.quill.aira.rag

import android.content.Context
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

class EpubTextExtractor(private val context: Context) {

    companion object {
        private const val MIN_CHAPTER_LENGTH = 50
        private val SCRIPT_REGEX = Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
        private val STYLE_REGEX = Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
        private val WHITESPACE_REGEX = Regex("[ \\t]+")
        private val NEWLINE_REGEX = Regex("\\n{3,}")
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

            publication.readingOrder.forEachIndexed { index, link ->
                val title = link.title ?: "spine[$index]"
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
                                chapterTitle = title,
                                text = plainText
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error parsing chapter $index ('$title')")
                }
            }

            publication.close()

        } catch (e: Exception) {
            Timber.e(e, "✗ Fatal error during EPUB extraction stream")
        }
    }.flowOn(Dispatchers.IO)

    private fun stripHtml(html: String): String =
        HtmlCompat.fromHtml(
            html.replace(SCRIPT_REGEX, " ").replace(STYLE_REGEX, " "),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        ).toString()
            .replace('\u00A0', ' ')
            .replace(WHITESPACE_REGEX, " ")
            .replace(NEWLINE_REGEX, "\n\n")
            .trim()
}