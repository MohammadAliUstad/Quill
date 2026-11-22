package com.yugentech.quill.di.modules.books

import com.yugentech.quill.insights.repository.InsightsRepository
import com.yugentech.quill.insights.repository.InsightsRepositoryImpl
import com.yugentech.quill.insights.viewmodel.InsightsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// The reading insights: stats from the library, reading sessions and Aira chats
val insightsModule = module {

    single<InsightsRepository> {
        InsightsRepositoryImpl(
            bookDao = get(),
            readingSessionDao = get(),
            airaMessageDao = get(),
            highlightDao = get(),
            visualDao = get()
        )
    }

    viewModel {
        InsightsViewModel(
            insightsRepository = get()
        )
    }
}