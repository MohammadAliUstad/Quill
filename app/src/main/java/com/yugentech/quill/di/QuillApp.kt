package com.yugentech.quill.di

import com.yugentech.theme.locale.LocaleContext
import android.app.Application
import android.content.Context
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yugentech.quill.BuildConfig
import com.yugentech.quill.di.modules.access.authModule
import com.yugentech.quill.di.modules.access.billingModule
import com.yugentech.quill.di.modules.access.userModule
import com.yugentech.quill.di.modules.books.booksModule
import com.yugentech.quill.di.modules.books.insightsModule
import com.yugentech.quill.di.modules.books.sourcesModule
import com.yugentech.quill.di.modules.books.storageModule
import com.yugentech.quill.di.modules.config.categoryModule
import com.yugentech.quill.di.modules.config.themeModule
import com.yugentech.quill.di.modules.config.settingsModule
import com.yugentech.quill.di.modules.config.announcementModule
import com.yugentech.quill.di.modules.core.cloudModule
import com.yugentech.quill.di.modules.notification.notificationModule
import com.yugentech.quill.di.modules.core.dataStoreModule
import com.yugentech.quill.di.modules.core.databaseModule
import com.yugentech.quill.di.modules.core.firebaseModule
import com.yugentech.quill.di.modules.core.networkModule
import com.yugentech.quill.di.modules.core.workerModule
import com.yugentech.quill.di.modules.shared.airaModule
import com.yugentech.quill.di.modules.shared.bookDetailsModule
import com.yugentech.quill.di.modules.shared.readerModule
import com.yugentech.quill.localization.AppLocale
import com.yugentech.quill.localization.AppStrings
import com.yugentech.quill.aira.localization.AiraStrings
import com.yugentech.quill.reader.localization.ReaderStrings
import com.yugentech.quill.notification.reminders.ReminderManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.koin.core.context.startKoin
import timber.log.Timber

class QuillApp : Application(), Configuration.Provider, ImageLoaderFactory {

    // Below Android 13, the language picked in the app (see AppLocale), so notifications and
    // anything else read from the app context follow it too; from 13 the system applies it
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        AppStrings.init(this)
        ReaderStrings.init(this)
        AiraStrings.init { LocaleContext.localized(this) }
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
                insightsModule,
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
                readerModule,
                notificationModule,
                settingsModule,
                announcementModule
            )
        }

        // Books the reminders from the saved settings (the hourly nudge job and the daily alarm).
        get<ReminderManager>().onAppStart()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    // Coil's default OkHttp client allows only 5 concurrent requests per host. Discover pulls
    // dozens of covers from standardebooks.org (HTTP/1.1), so raise the cap to stop them queueing.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .dispatcher(Dispatcher().apply { maxRequestsPerHost = 16 })
                    .build()
            }
            .build()
}