package com.yugentech.quill.di.modules.shared

import android.app.Application
import com.yugentech.quill.reader.settings.datastore.ReaderDataStore
import com.yugentech.quill.reader.settings.repository.ReaderSettingsRepository
import com.yugentech.quill.reader.settings.repository.ReaderSettingsRepositoryImpl
import com.yugentech.quill.reader.repository.book.ReaderBookRepository
import com.yugentech.quill.reader.repository.book.ReaderBookRepositoryImpl
import com.yugentech.quill.reader.sound.repository.BackgroundSoundRepository
import com.yugentech.quill.reader.sound.repository.BackgroundSoundRepositoryImpl
import com.yugentech.quill.reader.sound.service.BackgroundSoundService
import com.yugentech.quill.reader.repository.session.ReadingSessionRepository
import com.yugentech.quill.reader.repository.session.ReadingSessionRepositoryImpl
import com.yugentech.quill.reader.viewmodel.reader.ReaderViewModel
import com.yugentech.quill.reader.viewmodel.quick.QuickViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val readerModule = module {

    single {
        ReaderDataStore(
            dataStore = get(named("reader"))
        )
    }

    single<ReaderSettingsRepository> {
        ReaderSettingsRepositoryImpl(
            readerDataStore = get()
        )
    }

    single<ReaderBookRepository> {
        ReaderBookRepositoryImpl(
            bookDao = get(),
            highlightDao = get(),
            indexingStateDao = get()
        )
    }

    single<ReadingSessionRepository> {
        ReadingSessionRepositoryImpl(
            sessionDao = get()
        )
    }

    single {
        BackgroundSoundService(
            context = androidContext()
        )
    }

    single<BackgroundSoundRepository> {
        BackgroundSoundRepositoryImpl(
            soundService = get()
        )
    }

    viewModel {
        ReaderViewModel(
            application = androidContext() as Application,
            readerRepository = get(),
            sessionRepository = get(),
            preferencesRepository = get(),
            backgroundSoundRepository = get(),
            hapticService = get()
        )
    }

    viewModel {
        QuickViewModel(
            airaChatRepository = get(),
            quickRepository = get(),
            quotaRepository = get(),
            authRepository = get(),
            readerRepository = get(),
            billingRepository = get()
        )
    }
}
