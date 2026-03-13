package com.yugentech.quill.storage

import android.content.Context
import android.os.StatFs
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class StorageRepositoryImpl(
    private val context: Context,
    private val bookDao: BookDao
) : StorageRepository {

    override fun getTotalAppStorageUsed(): Flow<Long> {
        // Room returns Flow<Long?> if the table is empty, so we map null to 0L
        return bookDao.getTotalStorageUsed().map { it ?: 0L }
    }

    override fun getDownloadedBooksBySize(): Flow<List<BookEntity>> {
        return bookDao.getDownloadedBooksBySize()
    }

    override suspend fun removeDownload(bookId: String) {
        // 1. Delete the physical file from internal storage to free up space
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        
        // 2. Update the database (This will automatically trigger the UI to update!)
        bookDao.removeDownload(bookId)
    }

    override fun getDeviceFreeSpace(): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }

    override fun getDeviceTotalSpace(): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        return statFs.blockCountLong * statFs.blockSizeLong
    }
}