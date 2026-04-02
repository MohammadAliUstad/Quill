package com.yugentech.quill.di.modules

import com.yugentech.quill.bookDetails.worker.BookDownloadWorker
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.cloud.worker.SyncWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::BookDownloadWorker)
    workerOf(::BookEmbeddingWorker)
    workerOf(::SyncWorker)
}