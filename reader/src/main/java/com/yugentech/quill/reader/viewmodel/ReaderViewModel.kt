package com.yugentech.quill.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.reader.ReaderUiState
import com.yugentech.quill.reader.repository.ReaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

class ReaderViewModel(
    application: Application,
    private val readerRepository: ReaderRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var publication: Publication? = null

    // 1. Job to handle Debouncing (prevents database spamming)
    private var saveJob: Job? = null

    private var tocMap: Map<String, String> = emptyMap()

    fun loadBook(bookId: String, initialHref: String?) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Idle

            withContext(Dispatchers.IO) {
                try {
                    // --- SETUP PHASE ---
                    val bookEntity = readerRepository.getBook(bookId).firstOrNull()
                    val path = bookEntity?.localFilePath
                    val totalPages = bookEntity?.totalPages ?: 0

                    if (path == null) {
                        _uiState.value = ReaderUiState.Error("Book path not found.")
                        return@withContext
                    }

                    val file = File(path)
                    val context = getApplication<Application>()
                    val httpClient = DefaultHttpClient()
                    val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
                    val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
                    val publicationOpener =
                        PublicationOpener(parser, emptyList(), onCreatePublication = {})

                    val asset = assetRetriever.retrieve(file)
                        .getOrElse { error -> throw Exception("Failed to retrieve asset: $error") }

                    val pub = publicationOpener.open(asset, allowUserInteraction = false)
                        .getOrElse { error -> throw Exception("Failed to open publication: $error") }

                    publication = pub
                    val positions = pub.positions()
                    tocMap = flattenToc(pub.tableOfContents)

                    // --- PROFESSIONAL RESUME LOGIC ---

                    // 1. Explicit Navigation: Did the user click a specific chapter in the details screen?
                    val explicitLocator = initialHref?.let { href ->
                        Url.Companion(href)?.let { url ->
                            pub.linkWithHref(url)?.let { link ->
                                pub.locatorFromLink(link)
                            }
                        }
                    }

                    // 2. Saved Restoration: If no explicit chapter, try to load the exact saved paragraph.
                    val savedLocator = bookEntity.lastLocatorJson?.let { jsonStr ->
                        try {
                            // Recover the Locator from the stored JSON string
                            Locator.Companion.fromJSON(JSONObject(jsonStr))
                        } catch (_: Exception) {
                            null
                        }
                    }

                    // 3. Decision: Explicit takes priority. If null, use Saved. If both null, start at beginning.
                    val finalLocator = explicitLocator ?: savedLocator

                    _uiState.value = ReaderUiState.Success(
                        bookId = bookId,
                        publication = pub,
                        totalPages = totalPages,
                        allPositions = positions,
                        initialLocator = finalLocator
                    )

                } catch (e: Exception) {
                    _uiState.value = ReaderUiState.Error(e.message ?: "Failed to open book")
                }
            }
        }
    }

    fun saveProgress(bookId: String, locator: Locator) {
        // --- HANDLING CURIOSITY / FAST SCROLLING ---

        // 1. Cancel any previous save request that hasn't executed yet.
        // If the user is scrolling fast, this keeps getting cancelled.
        saveJob?.cancel()

        // 2. Start a new timer
        saveJob = viewModelScope.launch {
            // 3. Wait for 1 second of inactivity
            delay(2000)

            // 4. If we are still here (not cancelled), the user has stopped at this page.
            val pub = publication ?: return@launch

            val chapterIndex = pub.readingOrder
                .indexOfFirst { it.href == locator.href }
                .coerceAtLeast(0)


            val totalProgress = locator.locations.totalProgression?.toFloat() ?: 0f

            val chapterTitle = tocMap[locator.href.toString()]
                ?: pub.readingOrder.getOrNull(chapterIndex)?.title

            // 5. Convert Locator to JSON for exact precision
            val locatorJson = locator.toJSON().toString()

            readerRepository.saveProgress(
                bookId = bookId,
                progress = totalProgress,
                chapterTitle = chapterTitle,
                chapterIndex = chapterIndex,
                locatorJson = locatorJson
            )
        }
    }

    private fun flattenToc(links: List<Link>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        fun recurse(list: List<Link>) {
            for (link in list) {
                link.title?.let { title ->
                    map[link.href.toString()] = title
                }
                recurse(link.children)
            }
        }
        recurse(links)
        return map
    }

    override fun onCleared() {
        super.onCleared()
        publication?.close()
    }
}