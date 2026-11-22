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
        selectedText: String? = null,
        lastQuestion: String? = null,
        lastAnswer: String? = null
    ): String {
        val payload = hashMapOf(
            "query" to query,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "selectedText" to (selectedText ?: "")
        )
        // The previous exchange, so a follow-up like "why did she do that?" can be resolved
        if (!lastQuestion.isNullOrBlank()) {
            payload["lastQuestion"] = lastQuestion
            if (!lastAnswer.isNullOrBlank()) payload["lastAnswer"] = lastAnswer
        }

        val result = functions
            .getHttpsCallable("detectIntent")
            .call(payload)
            .await()

        val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from routing function")

        return response
    }
}