package com.yugentech.quill.aira.aira.generalChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.aira.generalChat.model.GeneralChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class GeneralChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getChatResponse(payload: GeneralChatPayload): String {
        return try {
            val result = functions
                .getHttpsCallable("generalChat")
                .call(payload.toMap())
                .await()

            (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from general chat function")
        } catch (e: Exception) {
            Timber.e(e, "GeneralChatService call failed")
            throw e
        }
    }
}
