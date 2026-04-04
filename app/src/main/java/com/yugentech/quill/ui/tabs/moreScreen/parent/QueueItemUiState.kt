package com.yugentech.quill.ui.tabs.moreScreen.parent

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

// The UI State holder
data class QueueItemUiState(
    val bookId: String,
    val title: String,
    val coverUrl: String?,
    val isRunning: Boolean,
    val progress: Int
)

class IndexingQueueViewModel(
    private val workManager: WorkManager,
    private val bookDao: BookDao
) : ViewModel() {

    // Observe the LiveData from WorkManager as a Kotlin Flow
    val queueState: StateFlow<List<QueueItemUiState>> = workManager
        .getWorkInfosByTagLiveData("AI_INDEXING")
        .asFlow()
        .map { workInfos ->
            // 1. Filter out finished, cancelled, or failed jobs
            val activeWorks = workInfos.filter {
                it.state == WorkInfo.State.RUNNING ||
                        it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.BLOCKED
            }

            // 2. Map the generic WorkInfo to our UI state
            activeWorks.mapNotNull { workInfo ->
                // Extract the exact book ID from the tag we set earlier ("index_12345")
                val bookId = workInfo.tags
                    .find { it.startsWith("index_") }
                    ?.removePrefix("index_") 
                    ?: return@mapNotNull null

                // Fetch the book title and cover from the DB
                val book = bookDao.getBookEntity(bookId) ?: return@mapNotNull null

                // Extract the real-time progress we broadcast from the Worker
                val progress = workInfo.progress.getInt(BookEmbeddingWorker.KEY_PROGRESS, 0)

                QueueItemUiState(
                    bookId = book.id,
                    title = book.title,
                    coverUrl = book.coverUrl,
                    isRunning = workInfo.state == WorkInfo.State.RUNNING,
                    progress = progress
                )
            }
            // 3. Sort so the currently RUNNING book is always at the top of the screen
            .sortedByDescending { it.isRunning }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}