package com.yugentech.quill.di.modules

import android.app.Application
import com.yugentech.quill.storage.StorageRepository
import com.yugentech.quill.storage.StorageRepositoryImpl
import com.yugentech.quill.storage.StorageViewModel
import com.yugentech.quill.ui.more.aboutScreen.parent.AboutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.scope.get
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

    viewModel {
        AboutViewModel(
            billingRepository = get()
        )
    }
}