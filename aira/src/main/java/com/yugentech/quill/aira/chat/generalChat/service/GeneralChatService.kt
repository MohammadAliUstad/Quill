package com.yugentech.quill.aira.chat.generalChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.generalChat.model.GeneralChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class GeneralChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getChatResponse(payload: GeneralChatPayload): String {
        Timber.d("GeneralChatService: Calling 'generalChat' function with payload: $payload")
        return try {
            val result = functions
                .getHttpsCallable("generalChat")
                .call(payload.toMap())
                .await()

            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            if (response == null) {
                Timber.e("GeneralChatService: Function returned null or invalid data")
                throw Exception("Empty response from general chat function")
            }

            Timber.d("GeneralChatService: Successfully received response (length: ${response.length})")
            response
        } catch (e: Exception) {
            Timber.e(e, "GeneralChatService: Function call failed")
            throw e
        }
    }
}