package com.yugentech.quill.di

import android.app.Application
import androidx.work.Configuration
import com.yugentech.quill.BuildConfig
import com.yugentech.quill.di.modules.shared.airaModule
import com.yugentech.quill.di.modules.access.authModule
import com.yugentech.quill.di.modules.access.billingModule
import com.yugentech.quill.di.modules.shared.bookDetailsModule
import com.yugentech.quill.di.modules.books.booksModule
import com.yugentech.quill.di.modules.core.cloudModule
import com.yugentech.quill.di.modules.core.dataStoreModule
import com.yugentech.quill.di.modules.core.databaseModule
import com.yugentech.quill.di.modules.books.gutenbergModule
import com.yugentech.quill.di.modules.core.networkModule
import com.yugentech.quill.di.modules.ai.quickActionModule
import com.yugentech.quill.di.modules.shared.readerModule
import com.yugentech.quill.di.modules.books.sourcesModule
import com.yugentech.quill.di.modules.storageModule
import com.yugentech.quill.di.modules.config.themeModule
import com.yugentech.quill.di.modules.access.userModule
import com.yugentech.quill.di.modules.core.firebaseModule
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
                firebaseModule,
                gutenbergModule,
                storageModule,
                sourcesModule,
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