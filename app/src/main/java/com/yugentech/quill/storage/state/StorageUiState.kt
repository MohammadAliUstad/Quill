package com.yugentech.quill.storage.state

import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookStorageBreakdown

data class StorageUiState(
    val downloadedBooks: List<BookEntity> = emptyList(),
    val bookStorageBreakdowns: Map<String, BookStorageBreakdown> = emptyMap(),
    val appStorageUsedBytes: Long = 0,
    val deviceFreeSpaceBytes: Long = 0,
    val deviceTotalSpaceBytes: Long = 0,
    val isLoading: Boolean = true
)