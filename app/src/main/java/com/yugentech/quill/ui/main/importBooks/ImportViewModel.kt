package com.yugentech.quill.ui.main.importBooks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The reader's own EPUBs coming into the library, from the dock's + button or the empty library. */
class ImportViewModel(
    private val bookDao: BookDao
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importResults = MutableStateFlow<List<ImportResult>>(emptyList())
    val importResults = _importResults.asStateFlow()

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
