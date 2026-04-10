package com.yugentech.quill.utils

import android.content.Context
import com.yugentech.quill.database.model.Chapter
import com.yugentech.theme.tokens.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber
import java.io.File
import java.util.Locale

class EpubParser(private val context: Context) {

    private val ignoredTitles = setOf(
        "titlepage", "imprint", "colophon", "uncopyright",
        "dedication", "epigraph", "endnotes", "bibliography",
        "index", "list of illustrations", "cover", "table of contents",
        "preface", "acknowledgments", "introduction", "loi"
    )

    suspend fun parse(filePath: String, bookTitle: String): ParsedEpub =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) return@withContext ParsedEpub(0, emptyList())

            try {
                val httpClient = DefaultHttpClient()
                val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
                val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
                val publicationOpener =
                    PublicationOpener(parser, emptyList(), onCreatePublication = {})

                val asset = assetRetriever.retrieve(file).getOrElse { throw Exception("$it") }
                val publication = publicationOpener.open(asset, allowUserInteraction = false)
                    .getOrElse { throw Exception("$it") }

                val positions: List<Locator> = publication.positions()
                val totalPages = positions.size.coerceAtLeast(1)
                val pageCountsByHref = positions
                    .groupingBy { it.href.toString().substringBefore("#") }
                    .eachCount()

                val allLinks = flattenLinks(publication.tableOfContents, 0)

                var globalIndex = 0
                val rawEntities = allLinks.mapNotNull { (link, depth) ->
                    val title = link.title?.trim() ?: AppConstants.EMPTY
                    val lowerTitle = title.lowercase(Locale.ROOT)

                    val isJunk = ignoredTitles.contains(lowerTitle)
                    val isBookTitle = title.equals(bookTitle, ignoreCase = true)

                    if (title.isNotBlank() && !isJunk && !isBookTitle) {

                        val cleanHref = link.href.toString().substringBefore("#")
                        val chapterPageCount = pageCountsByHref[cleanHref] ?: 0

                        Chapter(
                            title = title,
                            href = link.href.toString(),
                            index = globalIndex++,
                            depth = depth,
                            pageCount = chapterPageCount
                        )
                    } else {
                        null
                    }
                }

                val refinedChapters = rawEntities.map { entity ->
                    val lower = entity.title.trim().lowercase(Locale.ROOT)
                    val newDepth = when {
                        Regex("^part([\\s\\p{Z}]+|:|$)").containsMatchIn(lower) -> 0
                        Regex("^(book|volume)([\\s\\p{Z}]+|:|$)").containsMatchIn(lower) -> 1
                        else -> 2
                    }
                    entity.copy(depth = newDepth)
                }

                publication.close()

                return@withContext ParsedEpub(totalPages, refinedChapters)

            } catch (e: Exception) {
                Timber.Forest.e(e, "EpubParser failed")
                return@withContext ParsedEpub(0, emptyList())
            }
        }

    private fun flattenLinks(links: List<Link>, depth: Int): List<Pair<Link, Int>> {
        val flatList = mutableListOf<Pair<Link, Int>>()
        for (link in links) {
            flatList.add(link to depth)
            if (link.children.isNotEmpty()) {
                flatList.addAll(flattenLinks(link.children, depth + 1))
            }
        }
        return flatList
    }
}