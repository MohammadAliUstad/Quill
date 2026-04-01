package com.yugentech.quill.aira.quickPrompt.repository

import com.yugentech.quill.aira.quickPrompt.state.QuickPrompt
import com.yugentech.quill.aira.response.AiraResponse
import kotlinx.coroutines.flow.Flow

interface QuickPromptRepository {
    suspend fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse>
}