package com.yugentech.quill.dependencyInjection

import android.app.Application
import androidx.work.Configuration
import com.yugentech.quill.dependencyInjection.modules.booksModule
import com.yugentech.quill.dependencyInjection.modules.dataStoreModule
import com.yugentech.quill.dependencyInjection.modules.databaseModule
import com.yugentech.quill.dependencyInjection.modules.networkModule
import com.yugentech.quill.dependencyInjection.modules.themeModule
import com.yugentech.quill.dependencyInjection.modules.workerModule
import com.yugentech.quill.utils.ReleaseTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class QuillApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(ReleaseTree())

        startKoin {
            androidLogger()
            androidContext(this@QuillApp)
            workManagerFactory()

            modules(
                booksModule,
                dataStoreModule,
                databaseModule,
                themeModule,
                networkModule,
                workerModule
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()
}