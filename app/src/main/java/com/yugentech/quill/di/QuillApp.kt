package com.yugentech.quill.di

import android.app.Application
import androidx.work.Configuration
import com.yugentech.quill.BuildConfig
import com.yugentech.quill.di.modules.access.authModule
import com.yugentech.quill.di.modules.access.billingModule
import com.yugentech.quill.di.modules.access.userModule
import com.yugentech.quill.di.modules.books.booksModule
import com.yugentech.quill.di.modules.books.indexingModule
import com.yugentech.quill.di.modules.books.sourcesModule
import com.yugentech.quill.di.modules.books.storageModule
import com.yugentech.quill.di.modules.config.categoryModule
import com.yugentech.quill.di.modules.config.themeModule
import com.yugentech.quill.di.modules.core.cloudModule
import com.yugentech.quill.di.modules.core.dataStoreModule
import com.yugentech.quill.di.modules.core.databaseModule
import com.yugentech.quill.di.modules.core.firebaseModule
import com.yugentech.quill.di.modules.core.networkModule
import com.yugentech.quill.di.modules.core.workerModule
import com.yugentech.quill.di.modules.shared.airaModule
import com.yugentech.quill.di.modules.shared.bookDetailsModule
import com.yugentech.quill.di.modules.shared.readerModule
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
                billingModule,
                userModule,
                booksModule,
                indexingModule,
                sourcesModule,
                storageModule,
                categoryModule,
                themeModule,
                cloudModule,
                databaseModule,
                dataStoreModule,
                firebaseModule,
                networkModule,
                workerModule,
                airaModule,
                bookDetailsModule,
                readerModule
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}