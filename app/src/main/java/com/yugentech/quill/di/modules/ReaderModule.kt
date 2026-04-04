package com.yugentech.quill.di.modules

import android.app.Application
import com.yugentech.quill.reader.repository.ReaderDataStore
import com.yugentech.quill.reader.repository.ReaderPreferencesRepository
import com.yugentech.quill.reader.repository.ReaderPreferencesRepositoryImpl
import com.yugentech.quill.reader.repository.ReaderRepository
import com.yugentech.quill.reader.repository.ReaderRepositoryImpl
import com.yugentech.quill.reader.repository.ReadingSessionRepository
import com.yugentech.quill.reader.repository.ReadingSessionRepositoryImpl
import com.yugentech.quill.reader.viewmodel.ReaderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val readerModule = module {

    single {
        ReaderDataStore(
            get(named("reader"))
        )
    }

    single<ReaderPreferencesRepository> {
        ReaderPreferencesRepositoryImpl(
            readerDataStore = get()
        )
    }

    single<ReaderRepository> {
        ReaderRepositoryImpl(
            bookDao = get()
        )
    }

    single<ReadingSessionRepository> {
        ReadingSessionRepositoryImpl(
            dao = get()
        )
    }

    viewModel {
        ReaderViewModel(
            application = androidContext() as Application,
            readerRepository = get(),
            sessionRepository = get(),
            preferencesRepository = get()
        )
    }
}