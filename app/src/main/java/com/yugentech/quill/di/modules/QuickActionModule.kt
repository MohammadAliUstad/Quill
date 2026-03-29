package com.yugentech.quill.di.modules

import com.yugentech.quill.BuildConfig
import com.yugentech.quill.aira.quickPrompt.repository.QuickPromptRepository
import com.yugentech.quill.aira.quickPrompt.repository.QuickPromptRepositoryImpl
import com.yugentech.quill.aira.quickPrompt.viewmodel.QuickChatViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val quickActionModule = module {

    single<QuickPromptRepository> {
        QuickPromptRepositoryImpl(
            ragRetriever = get(),
            bookChunkDao = get(),
            bookDao = get(),
            geminiApiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    viewModel {
        QuickChatViewModel(
            quickPromptRepository = get(),
            quotaRepository = get(),
            authRepository = get()
        )
    }
}