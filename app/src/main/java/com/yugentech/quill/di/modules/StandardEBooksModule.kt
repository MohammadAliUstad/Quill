package com.yugentech.quill.di.modules

import com.yugentech.quill.standardEBooks.service.StandardApiService
import com.yugentech.quill.standardEBooks.repository.StandardRepository
import com.yugentech.quill.standardEBooks.repository.StandardRepositoryImpl
import com.yugentech.quill.standardEBooks.viewmodel.StandardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val standardEBooksModule = module {

    single {
        StandardApiService(
            httpClient = get()
        )
    }

    single<StandardRepository> {
        StandardRepositoryImpl(
            standardApi = get(),
            catalogDao = get(),
            categoryCacheDao = get()
        )
    }

    viewModel {
        StandardViewModel(
            standardRepository = get(),
            bookDetailsRepository = get()
        )
    }
}