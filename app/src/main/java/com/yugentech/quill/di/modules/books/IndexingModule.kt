package com.yugentech.quill.di.modules.books

import com.yugentech.quill.insghts.InsightsRepository
import com.yugentech.quill.insghts.InsightsRepositoryImpl
import com.yugentech.quill.insghts.InsightsViewModel
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