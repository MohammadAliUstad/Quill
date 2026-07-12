package com.yugentech.quill.di.modules.shared

import com.yugentech.quill.aira.service.AiraChatService
import com.yugentech.quill.aira.chat.bookChat.repository.BookChatRepository
import com.yugentech.quill.aira.chat.bookChat.repository.BookChatRepositoryImpl
import com.yugentech.quill.aira.chat.bookChat.service.BookChatService
import com.yugentech.quill.aira.chat.generalChat.repository.GeneralChatRepository
import com.yugentech.quill.aira.chat.generalChat.repository.GeneralChatRepositoryImpl
import com.yugentech.quill.aira.chat.generalChat.service.GeneralChatService
import com.yugentech.quill.aira.chat.quickChat.repository.QuickChatRepository
import com.yugentech.quill.aira.chat.quickChat.repository.QuickChatRepositoryImpl
import com.yugentech.quill.aira.chat.quickChat.service.QuickChatService
import com.yugentech.quill.aira.intent.repository.IntentDetectionRepository
import com.yugentech.quill.aira.intent.service.IntentDetectionService
import com.yugentech.quill.aira.rag.EmbeddingEngine
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.repository.AiraChatRepository
import com.yugentech.quill.aira.repository.AiraChatRepositoryImpl
import com.yugentech.quill.aira.util.VoiceOutputManager
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

    single {
        VoiceOutputManager(
            functions = get(),
            cacheDir = androidContext().cacheDir
        )
    }

    // --- Intent Detection ---
    single { IntentDetectionService(functions = get()) }
    single { IntentDetectionRepository(detectionService = get()) }

    // --- Services ---
    single { GeneralChatService(functions = get()) }
    single { BookChatService(functions = get()) }
    single { QuickChatService(functions = get()) }

    // --- Repositories ---
    single<BookChatRepository> { 
        BookChatRepositoryImpl(
            chatService = get(),
            ragRetriever = get()
        ) 
    }
    
    single<GeneralChatRepository> { 
        GeneralChatRepositoryImpl(
            chatService = get()
        ) 
    }
    
    single<QuickChatRepository> {
        QuickChatRepositoryImpl(
            bookDao = get(),
            bookChunkDao = get(),
            actionService = get(),
            ragRetriever = get()
        )
    }

    single {
        AiraChatService(
            intentDetectionRepository = get(),
            generalChatRepository = get(),
            bookChatRepository = get(),
            bookDao = get(),
            airaMessageDao = get()
        )
    }

    single<AiraChatRepository> {
        AiraChatRepositoryImpl(
            airaService = get(),
            airaMessageDao = get()
        )
    }
}
