package com.yugentech.quill.di.modules.books

import com.yugentech.quill.sources.discover.DiscoverViewModel
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepositoryImpl
import com.yugentech.quill.sources.gutenberg.service.GutenbergApiService
import com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergViewModel
import com.yugentech.quill.sources.standardEBooks.repository.StandardRepository
import com.yugentech.quill.sources.standardEBooks.repository.StandardRepositoryImpl
import com.yugentech.quill.sources.standardEBooks.service.StandardApiService
import com.yugentech.quill.sources.standardEBooks.viewmodel.StandardViewModel
import com.yugentech.quill.ui.tabs.sourcesScreen.parent.SourcesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sourcesModule = module {

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

    viewModel {
        SourcesViewModel(
            bookDao = get(),
            billingRepository = get()
        )
    }

    viewModel {
        DiscoverViewModel(
            standardRepository = get(),
            gutenbergRepository = get()
        )
    }
}