package com.yugentech.quill.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StorageUiState(
    val downloadedBooks: List<BookEntity> = emptyList(),
    val appStorageUsedBytes: Long = 0L,
    val deviceFreeSpaceBytes: Long = 0L,
    val deviceTotalSpaceBytes: Long = 0L,
    val isLoading: Boolean = true
)

class StorageViewModel(
    private val repository: StorageRepository
) : ViewModel() {

    // Combines multiple data sources into a single UI State stream
    val uiState: StateFlow<StorageUiState> = combine(
        repository.getDownloadedBooksBySize(),
        repository.getTotalAppStorageUsed()
    ) { books, usedBytes ->
        StorageUiState(
            downloadedBooks = books,
            appStorageUsedBytes = usedBytes,
            deviceFreeSpaceBytes = repository.getDeviceFreeSpace(),
            deviceTotalSpaceBytes = repository.getDeviceTotalSpace(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StorageUiState()
    )

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.removeDownload(bookId)
        }
    }
}