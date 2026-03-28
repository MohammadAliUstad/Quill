package com.yugentech.quill.reader.quickPrompt.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.reader.quickPrompt.util.QuickPromptHandler
import com.yugentech.quill.reader.quickPrompt.state.QuickPrompt
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow

class QuickPromptRepositoryImpl(
    private val geminiApiKey: String,
    private val ragRetriever: RagRetriever,
    private val bookChunkDao: BookChunkDao,
    private val bookDao: BookDao
) : QuickPromptRepository {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 4096
            }
        )
    }

    private val handler by lazy {
        QuickPromptHandler(
            model = model,
            ragRetriever = ragRetriever,
            bookDao = bookDao,
            bookChunkDao = bookChunkDao
        )
    }

    override suspend fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> {
        return handler.handle(bookId, quickPrompt)
    }
}