package com.yugentech.quill.di.modules

import com.yugentech.quill.BuildConfig
import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.aira.repository.AiraChatRepositoryImpl
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.aira.book.BookRepositoryImpl
import com.yugentech.quill.aira.rag.EmbeddingEngine
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.quick.viewmodel.QuickViewModel
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

    // 1. Provide the new BookRepository (Handles Book Metadata & Chunk Status)
    single<BookRepository> {
        BookRepositoryImpl(
            bookDao = get(),
            chunkDao = get(),
            indexingStateDao = get()
        )
    }

    single<AiraChatRepository> {
        AiraChatRepositoryImpl(
            geminiApiKey = BuildConfig.GEMINI_API_KEY,
            ragRetriever = get(),
            bookDao = get(),
            chunkDao = get(),
            airaMessageDao = get()
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
        )
    }

    viewModel {
        QuickViewModel(
            airaChatRepository = get(),
            quickRepository = get(),
            quotaRepository = get(),
            authRepository = get(),
            bookRepository = get(),
            billingRepository = get()
        )
    }
}