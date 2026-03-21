package com.yugentech.quill.aira.aira.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.yugentech.quill.aira.aira.viewmodel.AiraMessage
import com.yugentech.quill.aira.aira.util.LongPromptHandler
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiraChatRepositoryImpl(
    private val geminiApiKey: String,
    private val ragRetriever: RagRetriever,
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val airaMessageDao: AiraMessageDao
) : AiraChatRepository {

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

    private val expansionModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 0.0f
                maxOutputTokens = 2024
            }
        )
    }

    private val handler by lazy {
        LongPromptHandler(
            model = model,
            expansionModel = expansionModel,
            ragRetriever = ragRetriever,
            bookDao = bookDao,
            bookChunkDao = chunkDao,
            airaMessageDao = airaMessageDao
        )
    }

    override suspend fun ask(bookId: String, question: String): Flow<AiraResponse> =
        handler.ask(bookId, question)

    override suspend fun getMessagesForBook(bookId: String): Flow<List<AiraMessage>> =
        airaMessageDao.getMessagesForBook(bookId).map { entities ->
            entities.map { entity ->
                AiraMessage(
                    role = when (entity.role) {
                        AiraMessageRole.USER -> AiraMessage.Role.USER
                        AiraMessageRole.AIRA -> AiraMessage.Role.AIRA
                    },
                    content = entity.content,
                    timestamp = entity.timestamp
                )
            }
        }

    override suspend fun clearMessagesForBook(bookId: String) {
        airaMessageDao.clearMessagesForBook(bookId)
    }
}