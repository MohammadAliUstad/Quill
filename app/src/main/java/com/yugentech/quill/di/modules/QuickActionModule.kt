package com.yugentech.quill.di.modules

import com.yugentech.quill.BuildConfig
import com.yugentech.quill.aira.quick.repository.QuickRepository
import com.yugentech.quill.aira.quick.repository.QuickRepositoryImpl
import org.koin.dsl.module

val quickActionModule = module {

    single<QuickRepository> {
        QuickRepositoryImpl(
            ragRetriever = get(),
            bookChunkDao = get(),
            bookDao = get(),
            geminiApiKey = BuildConfig.GEMINI_API_KEY
        )
    }
}