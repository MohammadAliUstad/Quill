package com.yugentech.quill.aira.chat.quickChat.repository

import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.aira.response.AiraResponse
import kotlinx.coroutines.flow.Flow

interface QuickChatRepository {
    suspend fun ask(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse>
}
