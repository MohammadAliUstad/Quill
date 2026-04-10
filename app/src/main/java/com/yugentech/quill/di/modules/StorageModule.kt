package com.yugentech.quill.di.modules

import android.app.Application
import com.yugentech.quill.storage.StorageRepository
import com.yugentech.quill.storage.StorageRepositoryImpl
import com.yugentech.quill.storage.StorageViewModel
import com.yugentech.quill.ui.about.aboutScreen.parent.AboutViewModel
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

    viewModel {
        _root_ide_package_.com.yugentech.quill.ui.about.aboutScreen.parent.AboutViewModel(
            billingRepository = get()
        )
    }
}