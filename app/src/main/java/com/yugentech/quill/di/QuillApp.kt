package com.yugentech.quill.di

import android.app.Application
import android.webkit.WebView
import androidx.work.Configuration
import com.yugentech.quill.BuildConfig
import com.yugentech.quill.di.modules.airaModule
import com.yugentech.quill.di.modules.authModule
import com.yugentech.quill.di.modules.billingModule
import com.yugentech.quill.di.modules.bookDetailsModule
import com.yugentech.quill.di.modules.booksModule
import com.yugentech.quill.di.modules.cloudModule
import com.yugentech.quill.di.modules.dataStoreModule
import com.yugentech.quill.di.modules.databaseModule
import com.yugentech.quill.di.modules.gutenbergModule
import com.yugentech.quill.di.modules.networkModule
import com.yugentech.quill.di.modules.quickActionModule
import com.yugentech.quill.di.modules.readerModule
import com.yugentech.quill.di.modules.standardEBooksModule
import com.yugentech.quill.di.modules.storageModule
import com.yugentech.quill.di.modules.themeModule
import com.yugentech.quill.di.modules.userModule
import com.yugentech.quill.di.modules.workerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class QuillApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        WebView.setWebContentsDebuggingEnabled(true)

        startKoin {
            androidLogger()
            androidContext(this@QuillApp)
            workManagerFactory()

            modules(
                authModule,
                cloudModule,
                booksModule,
                dataStoreModule,
                databaseModule,
                themeModule,
                networkModule,
                workerModule,
                gutenbergModule,
                storageModule,
                standardEBooksModule,
                readerModule,
                airaModule,
                bookDetailsModule,
                quickActionModule,
                userModule,
                billingModule
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}