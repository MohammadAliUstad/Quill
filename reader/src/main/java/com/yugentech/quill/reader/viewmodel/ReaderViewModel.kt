package com.yugentech.quill.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.reader.datastore.ReaderPrefRepository
import com.yugentech.quill.reader.repository.ReaderRepository
import com.yugentech.quill.reader.session.ReadingSessionRepository
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubPreferences
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
    private val readerRepository: ReaderRepository,
    private val sessionRepository: ReadingSessionRepository,
    private val preferencesRepository: ReaderPrefRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val readerPreferences = preferencesRepository.readerPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderDefaults.getPreferences()
    )

    private var publication: Publication? = null
    private var saveJob: Job? = null
    private var tocMap: Map<String, String> = emptyMap()

    private var sessionStartTime: Long = 0L

    fun loadBook(bookId: String, initialHref: String?) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Idle

            withContext(Dispatchers.IO) {
                try {
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

                    val explicitLocator = initialHref?.let { href ->
                        Url.Companion(href)?.let { url ->
                            pub.linkWithHref(url)?.let { link ->
                                pub.locatorFromLink(link)
                            }
                        }
                    }

                    val savedLocator = bookEntity.lastLocatorJson?.let { jsonStr ->
                        try {
                            Locator.fromJSON(JSONObject(jsonStr))
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val finalLocator = explicitLocator ?: savedLocator

                    sessionStartTime = System.currentTimeMillis()

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
        saveJob?.cancel()

        saveJob = viewModelScope.launch {
            delay(2000)

            val pub = publication ?: return@launch

            val currentHref = locator.href.toString().substringBefore("#")
            val chapterIndex = pub.readingOrder
                .indexOfFirst { it.href.toString().substringBefore("#") == currentHref }
                .coerceAtLeast(0)

            val totalProgress = locator.locations.totalProgression?.toFloat() ?: 0f

            val chapterTitle = tocMap[locator.href.toString()]
                ?: pub.readingOrder.getOrNull(chapterIndex)?.title

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

    fun updatePreferences(newPreferences: EpubPreferences) {
        viewModelScope.launch {
            preferencesRepository.savePreferences(newPreferences)
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
        saveReadingSession()
        super.onCleared()
        publication?.close()
    }

    private fun saveReadingSession() {
        val currentState = _uiState.value
        if (currentState is ReaderUiState.Success && sessionStartTime > 0L) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - sessionStartTime

            if (duration > 10_000L) {
                CoroutineScope(Dispatchers.IO).launch {
                    sessionRepository.insertSession(
                        bookId = currentState.bookId,
                        startTime = sessionStartTime,
                        endTime = endTime
                    )
                }
            }
        }
    }
}