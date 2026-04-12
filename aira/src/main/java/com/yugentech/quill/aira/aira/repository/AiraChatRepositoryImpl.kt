package com.yugentech.quill.aira.aira.repository

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.aira.message.AiraMessage
import com.yugentech.quill.aira.aira.util.AiraHandler
import com.yugentech.quill.aira.aira.util.AiraResponder
import com.yugentech.quill.aira.aira.util.QueryRouter
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiraChatRepositoryImpl(
    private val functions: FirebaseFunctions,
    private val ragRetriever: RagRetriever,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao
) : AiraChatRepository {

    private val handler by lazy {
        AiraHandler(
            bookDao = bookDao,
            airaMessageDao = airaMessageDao,
            queryRouter = QueryRouter(functions),
            airaResponder = AiraResponder(functions, ragRetriever)
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