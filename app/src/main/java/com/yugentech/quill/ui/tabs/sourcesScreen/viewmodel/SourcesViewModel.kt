package com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.sources.standard.repository.StandardRepository
import com.yugentech.quill.ui.tabs.discoverScreen.util.toCoverThumbnailUrl
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.util.LocalBookImporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

private const val COVER_PREVIEW_COUNT = 6
private const val RECHECK_WHILE_DOWN_MILLIS = 60_000L

class SourcesViewModel(
    private val bookDao: BookDao,
    private val billingRepository: BillingRepository,
    private val standardRepository: StandardRepository,
    private val gutenbergRepository: GutenbergRepository,
    maintenanceMonitor: SourceMaintenanceMonitor
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importResults = MutableStateFlow<List<ImportResult>>(emptyList())
    val importResults = _importResults.asStateFlow()

    // How many library books came from each source, for the "N in your library" chips.
    val libraryCounts: StateFlow<Map<BookSource, Int>> = bookDao.getLibraryCountsBySource()
        .map { counts -> counts.associate { it.source to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // A few cover thumbnails per catalogue, read from the same caches Discover and the Gutenberg screen fill.
    val coverPreviews: StateFlow<Map<BookSource, List<String>>> = combine(
        standardRepository.getNewReleasesFlow(),
        gutenbergRepository.getPopularBooksFlow()
    ) { standardBooks, gutenbergBooks ->
        mapOf(
            BookSource.STANDARD_EBOOKS to standardBooks.coverSample(),
            BookSource.GUTENBERG to gutenbergBooks.coverSample()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** The catalogues down for maintenance, and when each said it'd be back: their cards close. */
    val sourcesDown: StateFlow<Map<BookSource, Instant?>> = maintenanceMonitor.down
        .map { down -> down.mapValues { it.value.backAfter } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            maintenanceMonitor.down.value.mapValues { it.value.backAfter }
        )

    init {
        // Ask each source's site itself, so its card can close before it's opened. While any is
        // down, ask it again every minute, so its card opens again by itself once it's back.
        // Apart, since the first check keeps retrying for a while when a site can't be reached
        viewModelScope.launch { maintenanceMonitor.checkAllIfDue() }
        viewModelScope.launch {
            maintenanceMonitor.down.collectLatest { down ->
                while (down.isNotEmpty()) {
                    delay(RECHECK_WHILE_DOWN_MILLIS)
                    down.keys.forEach { launch { maintenanceMonitor.check(it) } }
                }
            }
        }
        // Only hit the network when a cache is empty (e.g. Discover or Gutenberg never opened yet).
        viewModelScope.launch {
            if (standardRepository.getNewReleasesFlow().first().isEmpty()) {
                standardRepository.syncNewReleases()
            }
        }
        viewModelScope.launch {
            if (gutenbergRepository.getPopularBooksFlow().first().isEmpty()) {
                gutenbergRepository.syncPopularFeed()
            }
        }
    }

    private fun List<Book>.coverSample(): List<String> =
        mapNotNull { it.coverUrl?.toCoverThumbnailUrl() }.distinct().take(COVER_PREVIEW_COUNT)

    fun importFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true

            val results = LocalBookImporter.importFiles(
                context = context,
                bookDao = bookDao,
                uris = uris
            )

            // Each book that imported is queued for Aira, like a downloaded one
            val workManager = WorkManager.getInstance(context)
            results.filterIsInstance<ImportResult.Success>().forEach {
                BookEmbeddingWorker.enqueue(workManager, it.bookId)
            }

            _importResults.value = results
            _isImporting.value = false
        }
    }

    fun clearResults() {
        _importResults.value = emptyList()
    }
}