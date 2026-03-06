package com.yugentech.quill.di.modules

import com.yugentech.quill.bookDetails.worker.BookDownloadWorker
import com.yugentech.quill.aira.rag.BookIndexingWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workerModule = module {
    worker {
        BookDownloadWorker(
            context = get(),
            params = get(),
            bookDao = get(),
        )
    }

    worker {
        BookIndexingWorker(
            context = get(),
            params = get(),
            bookDao = get(),
            chunkDao = get(),
            embeddingEngine = get()
        )
    }
}