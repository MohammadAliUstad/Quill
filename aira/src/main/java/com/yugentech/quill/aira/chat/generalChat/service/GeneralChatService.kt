package com.yugentech.quill.aira.chat.generalChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.generalChat.model.GeneralChatPayload
import kotlinx.coroutines.tasks.await

class GeneralChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getChatResponse(payload: GeneralChatPayload): String {
        val result = functions
            .getHttpsCallable("generalChat")
            .call(payload.toMap())
            .await()

        val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from general chat function")

        return response
    }
}