package com.yugentech.quill.di.modules.core

import androidx.work.WorkManager
import com.yugentech.quill.aira.rag.BookEmbeddingWorker
import com.yugentech.quill.bookDetails.worker.BookDownloadWorker
import com.yugentech.quill.cloud.worker.SyncWorker
import com.yugentech.quill.notification.worker.PlayfulReminderWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {

    single {
        WorkManager.getInstance(androidContext())
    }

    workerOf(
        constructor = ::BookDownloadWorker
    )

    workerOf(
        constructor = ::BookEmbeddingWorker
    )

    workerOf(
        constructor = ::SyncWorker
    )

    workerOf(
        constructor = ::PlayfulReminderWorker
    )
}
