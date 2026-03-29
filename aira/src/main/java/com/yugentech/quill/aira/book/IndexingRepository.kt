package com.yugentech.quill.aira.book

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yugentech.quill.database.dao.BookChunkDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IndexingRepository(
    private val workManager: WorkManager,
    private val chunkDao: BookChunkDao
) {

    suspend fun isBookReady(bookId: String): Boolean =
        chunkDao.isBookIndexed(bookId)

    fun observeIndexing(bookId: String): Flow<WorkInfo?> =
        workManager
            .getWorkInfosForUniqueWorkFlow("book_pipeline_${bookId}")
            .map { it.firstOrNull() }
}