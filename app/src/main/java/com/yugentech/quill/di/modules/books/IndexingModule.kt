package com.yugentech.quill.di.modules.books

import com.yugentech.quill.insghts.repository.InsightsRepository
import com.yugentech.quill.insghts.repository.InsightsRepositoryImpl
import com.yugentech.quill.insghts.viewmodel.InsightsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val indexingModule = module {

    single<InsightsRepository> {
        InsightsRepositoryImpl(
            bookDao = get(),
            readingSessionDao = get(),
            airaMessageDao = get()
        )
    }

    viewModel {
        InsightsViewModel(
            insightsRepository = get()
        )
    }
}