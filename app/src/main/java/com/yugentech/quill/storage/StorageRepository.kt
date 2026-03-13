package com.yugentech.quill.storage

import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    // Database Flows
    fun getTotalAppStorageUsed(): Flow<Long>
    fun getDownloadedBooksBySize(): Flow<List<BookEntity>>
    
    // Actions
    suspend fun removeDownload(bookId: String)
    
    // Device Stats
    fun getDeviceFreeSpace(): Long
    fun getDeviceTotalSpace(): Long
}