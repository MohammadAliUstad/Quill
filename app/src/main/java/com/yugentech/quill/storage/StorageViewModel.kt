package com.yugentech.quill.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        // 1. Remove repository.getTotalAppStorageUsed() from here
        repository.getBookStorageBreakdowns()
    ) { books, breakdowns ->

        // 2. Calculate the TRUE total by summing all the parts from your breakdown!
        val trueTotalBytes = breakdowns.sumOf { breakdown ->
            breakdown.fileSizeBytes + breakdown.chunksBytes + breakdown.messagesBytes
        }

        StorageUiState(
            downloadedBooks = books,
            bookStorageBreakdowns = breakdowns.associateBy { it.bookId },
            appStorageUsedBytes = trueTotalBytes, // 3. Pass the true total here
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