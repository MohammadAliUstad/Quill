package com.yugentech.quill.storage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.storage.repository.StorageRepository
import com.yugentech.quill.storage.state.StorageUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StorageViewModel(
    private val repository: StorageRepository
) : ViewModel() {

    val uiState: StateFlow<StorageUiState> = combine(
        repository.getDownloadedBooksBySize(),
        repository.getBookStorageBreakdowns()
    ) { books, breakdowns ->

        val trueTotalBytes = breakdowns.sumOf { breakdown ->
            breakdown.fileSizeBytes + breakdown.chunksBytes + breakdown.messagesBytes
        }

        StorageUiState(
            downloadedBooks = books,
            bookStorageBreakdowns = breakdowns.associateBy { it.bookId },
            appStorageUsedBytes = trueTotalBytes,
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