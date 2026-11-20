package com.yugentech.quill.ui.info.indexing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.ui.info.indexing.state.IndexingUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class IndexingViewModel(
    workManager: WorkManager,
    private val bookDao: BookDao
) : ViewModel() {

    /** The books being prepared for Aira; null until WorkManager has answered once. */
    val queueState: StateFlow<List<IndexingUiState>?> = workManager
        .getWorkInfosByTagLiveData(BookEmbeddingWorker.TAG_ALL)
        .asFlow()
        .map { workInfos ->
            // Running, about to start, or waiting its turn in the queue
            val activeWorks = workInfos.filterNot { it.state.isFinished }

            val uiItems = mutableListOf<IndexingUiState>()

            for (workInfo in activeWorks) {
                val bookId = BookEmbeddingWorker.bookIdOf(workInfo.tags) ?: continue

                val book = bookDao.getBookEntity(bookId) ?: continue

                val progress = workInfo.progress.getInt(BookEmbeddingWorker.KEY_PROGRESS, 0)

                uiItems.add(
                    IndexingUiState(
                        bookId = book.id,
                        title = book.title,
                        coverUrl = book.coverUrl,
                        isRunning = workInfo.state == WorkInfo.State.RUNNING,
                        progress = progress
                    )
                )
            }

            // Running first, then one card per book: a book queued twice keeps its running entry
            uiItems
                .sortedByDescending { it.isRunning }
                .distinctBy { it.bookId }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Not "empty": the screen would flash the all-done state before the queue arrives
            initialValue = null
        )
}