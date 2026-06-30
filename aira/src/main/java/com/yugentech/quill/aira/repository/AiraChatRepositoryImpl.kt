package com.yugentech.quill.aira.repository

import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.service.AiraChatService
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiraChatRepositoryImpl(
    private val airaService: AiraChatService,
    private val airaMessageDao: AiraMessageDao
) : AiraChatRepository {

    override suspend fun ask(bookId: String, query: String): Flow<AiraResponse> =
        airaService.ask(bookId, query)

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