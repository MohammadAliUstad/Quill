package com.yugentech.quill.aira.chat.quickChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.quickChat.model.QuickChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class QuickChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getQuickChatResponse(payload: QuickChatPayload): String {
        Timber.d("QuickChatService: Calling 'quickChat' function with payload: $payload")
        return try {
            val result = functions
                .getHttpsCallable("quickChat")
                .call(payload.toMap())
                .await()

            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            if (response == null) {
                Timber.e("QuickChatService: Function returned null or invalid data")
                throw Exception("Empty response from quick chat function")
            }

            Timber.d("QuickChatService: Successfully received response (length: ${response.length})")
            response
        } catch (e: Exception) {
            Timber.e(e, "QuickChatService: Function call failed")
            throw e
        }
    }
}
