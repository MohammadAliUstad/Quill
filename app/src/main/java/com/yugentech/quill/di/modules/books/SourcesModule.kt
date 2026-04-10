package com.yugentech.quill.di.modules.books

import com.yugentech.quill.sources.discover.DiscoverViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sourcesModule = module {

    single {
        _root_ide_package_.com.yugentech.quill.sources.standardEBooks.service.StandardApiService(
            httpClient = get()
        )
    }

    single<com.yugentech.quill.sources.standardEBooks.repository.StandardRepository> {
        _root_ide_package_.com.yugentech.quill.sources.standardEBooks.repository.StandardRepositoryImpl(
            standardApi = get(),
            catalogDao = get(),
            categoryCacheDao = get()
        )
    }

    viewModel {
        _root_ide_package_.com.yugentech.quill.sources.standardEBooks.viewmodel.StandardViewModel(
            standardRepository = get(),
            bookDetailsRepository = get()
        )
    }

    single {
        _root_ide_package_.com.yugentech.quill.sources.gutenberg.service.GutenbergApiService(
            httpClient = get()
        )
    }

    single<com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository> {
        _root_ide_package_.com.yugentech.quill.sources.gutenberg.repository.GutenbergRepositoryImpl(
            apiService = get(),
            catalogDao = get()
        )
    }

    viewModel {
        _root_ide_package_.com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergViewModel(
            repository = get()
        )
    }

    viewModel {
        DiscoverViewModel(
            standardRepository = get(),
            gutenbergRepository = get()
        )
    }
}