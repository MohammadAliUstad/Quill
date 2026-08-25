package com.yugentech.quill.aira.intent.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class IntentDetectionService(
    private val functions: FirebaseFunctions
) {
    suspend fun detectIntent(
        query: String,
        bookTitle: String,
        bookAuthor: String,
        selectedText: String? = null
    ): String {
        val payload = hashMapOf(
            "query" to query,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "selectedText" to (selectedText ?: "")
        )

        val result = functions
            .getHttpsCallable("detectIntent")
            .call(payload)
            .await()

        val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from routing function")

        return response
    }
}