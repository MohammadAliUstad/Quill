package com.yugentech.quill.storage

import android.content.Context
import android.os.StatFs
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookStorageBreakdown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class StorageRepositoryImpl(
    private val context: Context,
    private val bookDao: BookDao
) : StorageRepository {

    override fun getDownloadedBooksBySize(): Flow<List<BookEntity>> {
        return bookDao.getDownloadedBooksBySize()
    }

    override suspend fun removeDownload(bookId: String) {
        val book = bookDao.getBookEntity(bookId)
        book?.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        
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

    override fun getBookStorageBreakdowns(): Flow<List<BookStorageBreakdown>> {
        return bookDao.getBookStorageBreakdown()
    }
}