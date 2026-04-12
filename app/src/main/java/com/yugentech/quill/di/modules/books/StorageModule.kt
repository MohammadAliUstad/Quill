package com.yugentech.quill.di.modules.books

import android.app.Application
import com.yugentech.quill.storage.repository.StorageRepository
import com.yugentech.quill.storage.repository.StorageRepositoryImpl
import com.yugentech.quill.storage.viewmodel.StorageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val storageModule = module {

    single<StorageRepository> {
        StorageRepositoryImpl(
            context = androidContext() as Application,
            bookDao = get()
        )
    }

    viewModel {
        StorageViewModel(
            repository = get()
        )
    }
}