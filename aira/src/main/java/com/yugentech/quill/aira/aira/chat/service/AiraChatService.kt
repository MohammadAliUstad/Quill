package com.yugentech.quill.aira.aira.chat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.aira.chat.model.ChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AiraChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getChatResponse(payload: ChatPayload): String {
        return try {
            val result = functions
                .getHttpsCallable("airaChat")
                .call(payload.toMap())
                .await()

            (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from chat function")
        } catch (e: Exception) {
            Timber.e(e, "AiraChatService call failed")
            throw e
        }
    }
}
