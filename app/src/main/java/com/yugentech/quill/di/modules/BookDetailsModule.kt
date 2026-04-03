package com.yugentech.quill.di.modules

import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.bookDetails.repository.BookDetailsRepositoryImpl
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bookDetailsModule = module {

    single<BookDetailsRepository> {
        BookDetailsRepositoryImpl(
            categoryDao = get(),
            bookDao = get(),
            workManager = get(),
            cloudSyncRepository = get()
        )
    }


    viewModel {
        BookDetailsViewModel(
            repository = get(),
            savedStateHandle = get()
        )
    }
}