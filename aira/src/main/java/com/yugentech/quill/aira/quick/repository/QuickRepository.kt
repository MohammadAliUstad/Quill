package com.yugentech.quill.aira.quick.repository

import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.response.AiraResponse
import kotlinx.coroutines.flow.Flow

interface QuickRepository {
    suspend fun ask(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse>
}