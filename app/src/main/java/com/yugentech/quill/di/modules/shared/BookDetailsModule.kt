package com.yugentech.quill.di.modules.shared

import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.bookDetails.repository.BookDetailsRepositoryImpl
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.ui.book.highlights.viewmodel.HighlightsViewModel
import com.yugentech.quill.ui.book.visuals.viewmodel.VisualsViewModel
import com.yugentech.quill.ui.book.airaChat.viewmodel.AiraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bookDetailsModule = module {

    single<BookDetailsRepository> {
        BookDetailsRepositoryImpl(
            categoryDao = get(),
            bookDao = get(),
            indexingStateDao = get(),
            workManager = get(),
            cloudSyncRepository = get()
        )
    }

    viewModel { params ->
        AiraViewModel(
            bookId = params.get(),
            airaChatRepository = get(),
            bookRepository = get(),
            quotaRepository = get(),
            authRepository = get(),
            workManager = get(),
            billingRepository = get()
        )
    }

    viewModel {
        BookDetailsViewModel(
            repository = get(),
            savedStateHandle = get(),
            maintenanceMonitor = get()
        )
    }

    viewModel {
        HighlightsViewModel(
            repository = get()
        )
    }

    viewModel {
        VisualsViewModel(
            visualDao = get()
        )
    }
}