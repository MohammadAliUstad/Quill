package com.yugentech.quill.di.modules.shared

import android.app.Application
import com.yugentech.quill.reader.pref.datastore.ReaderDataStore
import com.yugentech.quill.reader.pref.repository.ReaderPrefRepository
import com.yugentech.quill.reader.pref.repository.ReaderPrefRepositoryImpl
import com.yugentech.quill.reader.repository.ReaderRepository
import com.yugentech.quill.reader.repository.ReaderRepositoryImpl
import com.yugentech.quill.reader.service.BackgroundSoundService
import com.yugentech.quill.reader.session.ReadingSessionRepository
import com.yugentech.quill.reader.session.ReadingSessionRepositoryImpl
import com.yugentech.quill.reader.viewmodel.ReaderViewModel
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

    single<ReaderPrefRepository> {
        ReaderPrefRepositoryImpl(
            readerDataStore = get()
        )
    }

    single<ReaderRepository> {
        ReaderRepositoryImpl(
            bookDao = get(),
            highlightDao = get()
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

    viewModel {
        ReaderViewModel(
            application = androidContext() as Application,
            readerRepository = get(),
            sessionRepository = get(),
            preferencesRepository = get(),
            backgroundSoundService = get(),
            hapticService = get()
        )
    }
}