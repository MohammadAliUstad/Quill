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
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.util.LocalBookImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SourcesViewModel(
    private val bookDao: BookDao,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importResults = MutableStateFlow<List<ImportResult>>(emptyList())
    val importResults = _importResults.asStateFlow()

    fun importFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true

            // 1. Run the generic import logic
            val results = LocalBookImporter.importFiles(
                context = context,
                bookDao = bookDao,
                uris = uris
            )

            val isProUser = billingRepository.isPro.first()

            if (isProUser) {
                // Filter only the successfully imported books to be indexed
                results.filterIsInstance<ImportResult.Success>().forEach { success ->
                    scheduleBookIndexing(context, success.bookId)
                }
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