package com.yugentech.quill.di.modules

import com.yugentech.quill.BuildConfig
import com.yugentech.quill.aira.aira.repository.AiraRepository
import com.yugentech.quill.aira.aira.repository.AiraRepositoryImpl
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.aira.rag.EmbeddingEngine
import com.yugentech.quill.aira.rag.RagRetriever
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val airaModule = module {

    single {
        EmbeddingEngine(
            context = androidContext()
        )
    }

    single {
        RagRetriever(
            chunkDao = get(),
            bookDao = get(),
            embeddingEngine = get()
        )
    }

    single<AiraRepository> {
        AiraRepositoryImpl(
            ragRetriever = get(),
            chunkDao = get(),
            bookDao = get(),
            airaMessageDao = get(),
            geminiApiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    viewModel {
        AiraViewModel(
            airaRepository = get()
        )
    }
}