package com.yugentech.quill.di.modules

import android.app.Application
import com.yugentech.quill.reader.repository.ReaderRepository
import com.yugentech.quill.reader.repository.ReaderRepositoryImpl
import com.yugentech.quill.reader.repository.ReadingSessionRepository
import com.yugentech.quill.reader.repository.ReadingSessionRepositoryImpl
import com.yugentech.quill.reader.viewmodel.ReaderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val readerModule = module {

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
            sessionRepository = get()
        )
    }
}