package com.yugentech.quill.di.modules.shared

import com.yugentech.quill.aira.aira.util.AiraHandler
import com.yugentech.quill.aira.bookChat.handler.BookChatHandler
import com.yugentech.quill.aira.bookChat.service.BookChatService
import com.yugentech.quill.aira.generalChat.handler.GeneralChatHandler
import com.yugentech.quill.aira.generalChat.service.GeneralChatService
import com.yugentech.quill.aira.intentDetection.repository.IntentDetectionRepository
import com.yugentech.quill.aira.intentDetection.service.IntentDetectionService
import com.yugentech.quill.aira.quick.repository.QuickRepository
import com.yugentech.quill.aira.quick.repository.QuickRepositoryImpl
import com.yugentech.quill.aira.quick.service.QuickActionService
import com.yugentech.quill.aira.rag.EmbeddingEngine
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.repository.AiraChatRepositoryImpl
import org.koin.android.ext.koin.androidContext
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

    // --- Intent Detection ---
    single { IntentDetectionService(functions = get()) }
    single { IntentDetectionRepository(detectionService = get()) }

    // --- Chat Flows ---
    single { GeneralChatService(functions = get()) }
    single { GeneralChatHandler(chatService = get()) }
    single { BookChatService(functions = get()) }
    single { BookChatHandler(chatService = get(), ragRetriever = get()) }

    // --- Quick Action ---
    single { QuickActionService(functions = get()) }

    single {
        AiraHandler(
            intentDetectionRepository = get(),
            generalChatHandler = get(),
            bookChatHandler = get(),
            bookDao = get(),
            airaMessageDao = get()
        )
    }

    single<AiraChatRepository> {
        AiraChatRepositoryImpl(
            airaHandler = get(),
            airaMessageDao = get()
        )
    }

    single<QuickRepository> {
        QuickRepositoryImpl(
            quickActionService = get(),
            ragRetriever = get(),
            bookChunkDao = get(),
            bookDao = get()
        )
    }
}
