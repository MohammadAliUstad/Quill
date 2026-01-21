package com.yugentech.quill.dependencyInjection.modules

import com.yugentech.quill.workmanager.BookDownloadWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workerModule = module {
    worker {
        BookDownloadWorker(
            context = get(),
            params = get(),
            libraryBooksDao = get(),
            bookDetailsDao = get()
        )
    }
}