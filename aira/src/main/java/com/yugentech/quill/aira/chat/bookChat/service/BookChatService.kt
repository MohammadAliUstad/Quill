package com.yugentech.quill.aira.chat.bookChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.bookChat.payload.BookChatPayload
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class BookChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun bookChat(payload: BookChatPayload): String {
        Timber.d("BookChatService: Calling 'bookChat' function with payload: $payload")
        return try {
            val result = functions
                .getHttpsCallable("bookChat")
                .call(payload.toMap())
                .await()

            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            if (response == null) {
                Timber.e("BookChatService: Function returned null or invalid data")
                throw Exception("Empty response from book chat function")
            }
            
            Timber.d("BookChatService: Successfully received response (length: ${response.length})")
            response
        } catch (e: Exception) {
            Timber.e(e, "BookChatService: Function call failed")
            throw e
        }
    }
}