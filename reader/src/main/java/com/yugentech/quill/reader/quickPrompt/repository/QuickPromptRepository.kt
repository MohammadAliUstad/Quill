package com.yugentech.quill.reader.quickPrompt.repository

import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.reader.quickPrompt.state.QuickPrompt
import kotlinx.coroutines.flow.Flow

interface QuickPromptRepository {
    suspend fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse>
}