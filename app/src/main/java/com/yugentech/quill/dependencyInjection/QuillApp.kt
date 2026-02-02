package com.yugentech.quill.dependencyInjection

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yugentech.quill.dependencyInjection.modules.alertsModule
import com.yugentech.quill.dependencyInjection.modules.authModule
import com.yugentech.quill.dependencyInjection.modules.dataStoreModule
import com.yugentech.quill.dependencyInjection.modules.databaseModule
import com.yugentech.quill.dependencyInjection.modules.notificationModule
import com.yugentech.quill.dependencyInjection.modules.sessionModule
import com.yugentech.quill.dependencyInjection.modules.themeModule
import com.yugentech.quill.dependencyInjection.modules.timerModule
import com.yugentech.quill.dependencyInjection.modules.userModule
import com.yugentech.quill.dependencyInjection.modules.viewModelModule
import com.yugentech.quill.utils.ReleaseTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

// Main Application class responsible for global initialization
class QuillApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure logging: DebugTree for development, ReleaseTree for production
        Timber.plant(ReleaseTree())

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // Start Koin dependency injection and load all modules
        startKoin {
            androidLogger()
            androidContext(this@QuillApp)
            modules(
                dataStoreModule,
                authModule,
                databaseModule,
                sessionModule,
                userModule,
                themeModule,
                viewModelModule,
                alertsModule,
                timerModule,
                notificationModule
            )
        }
    }
}