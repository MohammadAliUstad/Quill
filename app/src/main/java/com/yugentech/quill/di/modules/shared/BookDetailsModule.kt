package com.yugentech.quill.di.modules.shared

import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.bookDetails.repository.BookDetailsRepositoryImpl
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.ui.shared.bookDetails.parent.HighlightsViewModel
import com.yugentech.quill.ui.shared.airaChat.viewmodel.AiraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bookDetailsModule = module {

    single<BookDetailsRepository> {
        BookDetailsRepositoryImpl(
            categoryDao = get(),
            bookDao = get(),
            chunkDao = get(),
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
            billingRepository = get()
        )
    }

    viewModel {
        HighlightsViewModel(
            repository = get()
        )
    }
}