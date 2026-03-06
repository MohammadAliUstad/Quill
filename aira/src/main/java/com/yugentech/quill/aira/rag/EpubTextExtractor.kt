package com.yugentech.quill.aira.rag

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

data class ChapterText(
    val chapterIndex: Int,    // spine index (raw, as-is from readingOrder)
    val chapterTitle: String, // title from spine link, or fallback
    val text: String
)

class EpubTextExtractor(private val context: Context) {

    companion object {
        private const val TAG = "QuillExtractor"
        private const val MIN_CHAPTER_LENGTH = 50
    }

    suspend fun extract(filePath: String): List<ChapterText> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "File not found at $filePath")
            return@withContext emptyList()
        }

        try {
            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
            val publicationOpener = PublicationOpener(parser, emptyList(), onCreatePublication = {})

            val asset = assetRetriever.retrieve(file).getOrElse { err ->
                Log.e(TAG, "Failed to retrieve asset — $err")
                return@withContext emptyList()
            }

            val publication = publicationOpener.open(asset, allowUserInteraction = false)
                .getOrElse { err ->
                    Log.e(TAG, "Failed to open publication — $err")
                    return@withContext emptyList()
                }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "EPUB spine — ${publication.readingOrder.size} total spine items:")

            val chapters = mutableListOf<ChapterText>()

            publication.readingOrder.forEachIndexed { index, link ->
                val title = link.title ?: "spine[$index]"
                val href = link.url().toString()

                try {
                    val resource = publication.get(link)
                    if (resource == null) {
                        Log.w(TAG, "  [$index] \"$title\" ($href) → SKIPPED (null resource)")
                        return@forEachIndexed
                    }

                    val bytes = resource.read().getOrElse { err ->
                        Log.w(TAG, "  [$index] \"$title\" ($href) → SKIPPED (read error: $err)")
                        return@forEachIndexed
                    }

                    val html = String(bytes, Charsets.UTF_8)
                    val plainText = stripHtml(html).trim()

                    if (plainText.length >= MIN_CHAPTER_LENGTH) {
                        chapters.add(
                            ChapterText(
                                chapterIndex = index,
                                chapterTitle = title,
                                text = plainText
                            )
                        )
                        Log.d(TAG, "  [$index] \"$title\" → INDEXED (${plainText.length} chars) — \"${plainText.take(60).replace('\n', ' ')}...\"")
                    } else {
                        Log.d(TAG, "  [$index] \"$title\" → SKIPPED (${plainText.length} chars < $MIN_CHAPTER_LENGTH, likely front/backmatter)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "  [$index] \"$title\" → ERROR: ${e.message}")
                }
            }

            publication.close()

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "Extraction summary: ${chapters.size} spine items indexed out of ${publication.readingOrder.size} total")
            Log.d(TAG, "Indexed chapter indices: ${chapters.map { it.chapterIndex }}")
            Log.d(TAG, "Indexed chapter titles : ${chapters.map { "\"${it.chapterTitle}\"" }}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            chapters

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during extraction: ${e.message}", e)
            emptyList()
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</(p|div|h[1-6]|li|br|tr|blockquote)[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("\u00AD", "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}