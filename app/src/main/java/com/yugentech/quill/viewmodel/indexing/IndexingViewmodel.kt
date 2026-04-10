package com.yugentech.quill.viewmodel.indexing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class IndexingViewModel(
    workManager: WorkManager,
    private val bookDao: BookDao
) : ViewModel() {

    val queueState: StateFlow<List<IndexingUiState>> = workManager
        .getWorkInfosByTagLiveData("AI_INDEXING")
        .asFlow()
        .map { workInfos ->
            val activeWorks = workInfos.filter {
                it.state == WorkInfo.State.RUNNING ||
                        it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.BLOCKED
            }

            val uiItems = mutableListOf<IndexingUiState>()

            for (workInfo in activeWorks) {
                val bookId = workInfo.tags
                    .find { it.startsWith("index_") }
                    ?.removePrefix("index_")
                    ?: continue

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

            uiItems
                .distinctBy { it.bookId }
                .sortedByDescending { it.isRunning }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}