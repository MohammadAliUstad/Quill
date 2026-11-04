package com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.sources.standard.repository.StandardRepository
import com.yugentech.quill.ui.tabs.discoverScreen.util.toCoverThumbnailUrl
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.util.LocalBookImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val COVER_PREVIEW_COUNT = 6

class SourcesViewModel(
    private val bookDao: BookDao,
    private val billingRepository: BillingRepository,
    private val standardRepository: StandardRepository,
    private val gutenbergRepository: GutenbergRepository
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

    init {
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

            // 1. Run the generic import logic
            val results = LocalBookImporter.importFiles(
                context = context,
                bookDao = bookDao,
                uris = uris
            )

            // Filter only the successfully imported books to be indexed
            results.filterIsInstance<ImportResult.Success>().forEach { success ->
                scheduleBookIndexing(context, success.bookId)
            }

            _importResults.value = results
            _isImporting.value = false
        }
    }

    private fun scheduleBookIndexing(context: Context, bookId: String) {
        val indexRequest = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
            .setInputData(
                workDataOf(BookEmbeddingWorker.Companion.KEY_BOOK_ID to bookId)
            )
            .addTag("index_$bookId")
            .addTag("AI_INDEXING")
            .build()

        // This correctly queues the indexing sequentially as you designed
        WorkManager.Companion.getInstance(context).enqueueUniqueWork(
            "global_book_processing_queue",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            indexRequest
        )
    }

    fun clearResults() {
        _importResults.value = emptyList()
    }
}