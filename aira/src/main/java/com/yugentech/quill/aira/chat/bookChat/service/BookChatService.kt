package com.yugentech.quill.aira.chat.bookChat.service

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.chat.bookChat.payload.BookChatPayload
import kotlinx.coroutines.tasks.await

class BookChatService(
    private val functions: FirebaseFunctions
) {
    suspend fun bookChat(payload: BookChatPayload): String {
        val result = functions
            .getHttpsCallable("bookChat")
            .call(payload.toMap())
            .await()

        val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from book chat function")

        return response
    }
}