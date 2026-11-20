package com.yugentech.quill.storage.repository

import android.content.Context
import android.os.StatFs
import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.BookStorageBreakdown
import kotlinx.coroutines.flow.Flow
import java.io.File

class StorageRepositoryImpl(
    private val context: Context,
    private val bookDao: BookDao,
    private val bookDetailsRepository: BookDetailsRepository
) : StorageRepository {

    override fun getDownloadedBooksBySize(): Flow<List<BookEntity>> {
        return bookDao.getDownloadedBooksBySize()
    }

    override suspend fun removeDownload(bookId: String) {
        val book = bookDao.getBookEntity(bookId)

        // An imported book can't be downloaded again: without its file it would be left in the
        // library as an empty shell, so it's removed completely, as from its details screen
        if (book?.source == BookSource.USER_IMPORTED) {
            bookDetailsRepository.deleteBook(bookId)
            return
        }

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