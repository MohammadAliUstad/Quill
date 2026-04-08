package com.yugentech.quill.storage

import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookStorageBreakdown
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getDownloadedBooksBySize(): Flow<List<BookEntity>>
    suspend fun removeDownload(bookId: String)
    fun getDeviceFreeSpace(): Long
    fun getDeviceTotalSpace(): Long
    fun getBookStorageBreakdowns(): Flow<List<BookStorageBreakdown>>
}