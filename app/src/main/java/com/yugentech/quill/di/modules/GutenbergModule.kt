package com.yugentech.quill.di.modules

import com.yugentech.quill.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.gutenberg.repository.GutenbergRepositoryImpl
import com.yugentech.quill.gutenberg.service.GutenbergApiService
import com.yugentech.quill.gutenberg.viewmodel.GutenbergViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val gutenbergModule = module {

    single {
        GutenbergApiService(
            httpClient = get()
        )
    }

    single<GutenbergRepository> {
        GutenbergRepositoryImpl(
            apiService = get(),
            catalogDao = get()
        )
    }

    viewModel {
        GutenbergViewModel(
            repository = get()
        )
    }
}