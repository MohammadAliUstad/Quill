package com.yugentech.quill.aira.chat.quickChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.quickChat.model.QuickChatPayload
import kotlinx.coroutines.tasks.await

class QuickChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getQuickChatResponse(payload: QuickChatPayload): String {
        val result = functions
            .getHttpsCallable("quickChat")
            .call(payload.toMap())
            .await()

        val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from quick chat function")

        return response
    }
}
