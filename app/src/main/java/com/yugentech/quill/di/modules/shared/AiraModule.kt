package com.yugentech.quill.di.modules.shared

import com.yugentech.quill.aira.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.aira.repository.AiraChatRepositoryImpl
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.aira.book.BookRepositoryImpl
import com.yugentech.quill.aira.quick.repository.QuickRepository
import com.yugentech.quill.aira.quick.repository.QuickRepositoryImpl
import com.yugentech.quill.aira.quick.viewmodel.QuickViewModel
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

    single<BookRepository> {
        BookRepositoryImpl(
            bookDao = get(),
            chunkDao = get(),
            indexingStateDao = get(),
            workManager = get()
        )
    }

    single<AiraChatRepository> {
        AiraChatRepositoryImpl(
            functions = get(),
            ragRetriever = get(),
            bookDao = get(),
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
            billingRepository = get(),
        )
    }

    single<QuickRepository> {
        QuickRepositoryImpl(
            ragRetriever = get(),
            bookChunkDao = get(),
            bookDao = get(),
            functions = get()
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