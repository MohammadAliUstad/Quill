package com.yugentech.quill.aira.aira.bookChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.aira.bookChat.model.BookChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class BookChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun getChatResponse(payload: BookChatPayload): String {
        return try {
            val result = functions
                .getHttpsCallable("bookChat")
                .call(payload.toMap())
                .await()

            (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from book chat function")
        } catch (e: Exception) {
            Timber.e(e, "BookChatService call failed")
            throw e
        }
    }
}
