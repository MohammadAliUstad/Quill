package com.yugentech.quill.di.modules.books

import com.yugentech.quill.sources.discover.viewmodel.DiscoverViewModel
import com.yugentech.quill.sources.shared.SourceMaintenanceMonitor
import com.yugentech.quill.sources.standard.repository.StandardRepository
import com.yugentech.quill.sources.standard.repository.StandardRepositoryImpl
import com.yugentech.quill.sources.standard.service.StandardApiService
import com.yugentech.quill.sources.discover.viewmodel.ShelfViewModel
import com.yugentech.quill.ui.main.importBooks.ImportViewModel
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

    viewModel { (shelf: String) ->
        ShelfViewModel(
            shelf = shelf,
            standardRepository = get(),
            bookDetailsRepository = get(),
            maintenanceMonitor = get()
        )
    }

    // Which sources are down for maintenance, shared by every screen that shows it
    single {
        SourceMaintenanceMonitor(
            standardRepository = get()
        )
    }

    viewModel {
        ImportViewModel(
            bookDao = get()
        )
    }

    viewModel {
        DiscoverViewModel(
            standardRepository = get(),
            maintenanceMonitor = get()
        )
    }
}