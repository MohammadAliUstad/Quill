package com.yugentech.quill.reader.viewmodel.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.reader.settings.model.ReaderSettings
import com.yugentech.quill.reader.settings.repository.ReaderSettingsRepository
import com.yugentech.quill.reader.repository.book.ReaderBookRepository
import com.yugentech.quill.reader.sound.repository.BackgroundSoundRepository
import com.yugentech.quill.reader.repository.session.ReadingSessionRepository
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
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
import java.util.UUID

sealed class ReaderCommand {
    object NextPage : ReaderCommand()
    object PreviousPage : ReaderCommand()
}

class ReaderViewModel(
    application: Application,
    private val readerRepository: ReaderBookRepository,
    private val sessionRepository: ReadingSessionRepository,
    private val preferencesRepository: ReaderSettingsRepository,
    private val backgroundSoundRepository: BackgroundSoundRepository,
    private val hapticService: HapticService
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _highlights = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val highlights = _highlights.asStateFlow()

    private val _activeSound = MutableStateFlow<BackgroundSound>(BackgroundSound.NONE)
    val activeSound = _activeSound.asStateFlow()

    private val _soundVolume = MutableStateFlow(1.0f)
    val soundVolume = _soundVolume.asStateFlow()

    val readerPreferences = preferencesRepository.readerSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReaderSettings(ReaderDefaults.getPreferences())
    )

    private val _commands = Channel<ReaderCommand>(Channel.BUFFERED)
    val commands = _commands.receiveAsFlow()

    private var publication: Publication? = null
    private var saveJob: Job? = null
    private var highlightsJob: Job? = null
    private var tocMap: Map<String, String> = emptyMap()

    private var sessionStartTime: Long = 0L

    fun loadBook(bookId: String, initialHref: String?, locatorJson: String? = null) {
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

                    val targetLocator = locatorJson?.let { jsonStr ->
                        try {
                            Locator.fromJSON(JSONObject(jsonStr))
                        } catch (_: Exception) {
                            null
                        }
                    }

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

                    val finalLocator = targetLocator ?: explicitLocator ?: savedLocator

                    sessionStartTime = System.currentTimeMillis()

                    _uiState.value = ReaderUiState.Success(
                        bookId = bookId,
                        publication = pub,
                        totalPages = totalPages,
                        allPositions = positions,
                        initialLocator = finalLocator
                    )

                    observeHighlights(bookId)

                    // Auto-play sound if enabled
                    viewModelScope.launch {
                        val prefs = preferencesRepository.readerSettings.first()
                        _soundVolume.value = prefs.soundVolume
                        if (prefs.autoPlaySound && prefs.lastSelectedSound != BackgroundSound.NONE) {
                            playBackgroundSound(prefs.lastSelectedSound)
                        }
                    }

                } catch (e: Exception) {
                    _uiState.value = ReaderUiState.Error(e.message ?: "Failed to open book")
                }
            }
        }
    }

    private fun observeHighlights(bookId: String) {
        highlightsJob?.cancel()
        highlightsJob = viewModelScope.launch {
            readerRepository.getHighlights(bookId).collect { highlightList ->
                _highlights.value = highlightList
            }
        }
    }

    fun addHighlight(
        bookId: String,
        locatorJson: String,
        colorInt: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newEntity = HighlightEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                locatorJson = locatorJson,
                colorInt = colorInt,
                createdAt = System.currentTimeMillis()
            )
            readerRepository.saveHighlight(newEntity)
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            readerRepository.deleteHighlight(highlightId)
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

    fun updateEpubPreferences(newPreferences: EpubPreferences) {
        viewModelScope.launch {
            preferencesRepository.saveEpubPreferences(newPreferences)
        }
    }

    fun updateVolumeNavigation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveVolumeNavigation(enabled)
        }
    }

    fun updateNightLight(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveNightLight(enabled)
        }
    }

    fun updateAutoPlaySound(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveAutoPlaySound(enabled)
        }
    }

    fun onVolumeUp() {
        viewModelScope.launch {
            _commands.send(ReaderCommand.PreviousPage)
        }
    }

    fun onVolumeDown() {
        viewModelScope.launch {
            _commands.send(ReaderCommand.NextPage)
        }
    }

    fun toggleBackgroundSound(sound: BackgroundSound) {
        if (_activeSound.value == sound) {
            stopBackgroundSound()
        } else {
            playBackgroundSound(sound)
        }
    }

    private fun playBackgroundSound(sound: BackgroundSound) {
        backgroundSoundRepository.play(sound, _soundVolume.value)
        _activeSound.value = sound
        if (sound != BackgroundSound.NONE) {
            viewModelScope.launch {
                preferencesRepository.saveLastSelectedSound(sound)
            }
        }
    }

    private fun stopBackgroundSound() {
        backgroundSoundRepository.stop()
        _activeSound.value = BackgroundSound.NONE
    }

    fun quickToggleSound() {
        val lastSound = readerPreferences.value.lastSelectedSound
        if (_activeSound.value == BackgroundSound.NONE) {
            toggleBackgroundSound(lastSound)
        } else {
            toggleBackgroundSound(_activeSound.value)
        }
    }

    fun updateSoundVolume(volume: Float) {
        _soundVolume.value = volume
        backgroundSoundRepository.setVolume(volume)
        viewModelScope.launch {
            preferencesRepository.saveSoundVolume(volume)
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
        backgroundSoundRepository.stop()
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