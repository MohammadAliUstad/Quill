package com.yugentech.quill.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.bookDetails.BookDetailsRepository
import com.yugentech.quill.library.LibraryRepository
import com.yugentech.quill.ui.dash.screens.ReaderUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber
import java.io.File

class ReaderViewModel(
    application: Application,
    private val bookDetailsRepository: BookDetailsRepository,
    private val libraryRepository: LibraryRepository // 1. INJECT REPOSITORY
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState = _uiState.asStateFlow()

    var publication: Publication? = null
        private set

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading

            withContext(Dispatchers.IO) {
                try {
                    // 1. Fetch path from DB
                    val details = bookDetailsRepository.getDetails(bookId).firstOrNull()
                    val path = details?.localFilePath

                    if (path == null) {
                        _uiState.value = ReaderUiState.Error("Book path not found in DB.")
                        return@withContext
                    }

                    val file = File(path)
                    if (!file.exists()) {
                        _uiState.value = ReaderUiState.Error("File missing: ${file.absolutePath}")
                        return@withContext
                    }

                    // 2. Initialize Readium
                    val context = getApplication<Application>()
                    val httpClient = DefaultHttpClient()
                    val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

                    val parser = DefaultPublicationParser(
                        context = context,
                        httpClient = httpClient,
                        assetRetriever = assetRetriever,
                        pdfFactory = null
                    )

                    val publicationOpener = PublicationOpener(
                        publicationParser = parser,
                        contentProtections = emptyList(),
                        onCreatePublication = {}
                    )

                    // 3. Open Publication
                    assetRetriever.retrieve(file)
                        .onSuccess { asset ->
                            publicationOpener.open(asset, allowUserInteraction = false)
                                .onSuccess { pub ->
                                    Timber.d("Readium: Opened successfully")
                                    publication = pub
                                    _uiState.value = ReaderUiState.Success(pub)
                                }
                                .onFailure { error ->
                                    Timber.e("Readium open failed: %s", error)
                                    _uiState.value = ReaderUiState.Error("Open Failed: $error")
                                }
                        }
                        .onFailure { error ->
                            _uiState.value = ReaderUiState.Error("Asset Retrieval Failed: $error")
                        }

                } catch (e: Exception) {
                    Timber.e(e, "Reader crash")
                    _uiState.value = ReaderUiState.Error("Crash: ${e.message}")
                }
            }
        }
    }

    /**
     * Calculates the current progress and saves it to the database.
     * Called whenever the user turns a page or scrolls.
     */
    fun saveProgress(bookId: String, locator: Locator) {
        viewModelScope.launch {
            val pub = publication ?: return@launch

            // A. Find Chapter Index (index in readingOrder)
            val chapterIndex = pub.readingOrder.indexOfFirst { it.href == locator.href }
                .takeIf { it >= 0 } ?: 0

            // B. Get Total Progress (0.0 to 1.0)
            // totalProgression is best, fallback to chapter progression
            val totalProgress = locator.locations.totalProgression?.toFloat()
                ?: locator.locations.progression?.toFloat()
                ?: 0f

            // C. Save to Database
            libraryRepository.updateProgress(
                bookId = bookId,
                chapter = chapterIndex,
                scroll = 0, // Not strictly needed if relying on Locator/Percent
                percent = totalProgress
            )

            Timber.d("Progress Saved: ${(totalProgress * 100).toInt()}% (Chapter $chapterIndex)")
        }
    }

    override fun onCleared() {
        super.onCleared()
        publication?.close()
    }
}