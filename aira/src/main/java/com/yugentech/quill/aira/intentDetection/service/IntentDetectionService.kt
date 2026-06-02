package com.yugentech.quill.aira.aira.intentDetection.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class IntentDetectionService(
    private val functions: FirebaseFunctions
) {
    suspend fun getRoutingDecision(
        prompt: String,
        bookTitle: String,
        bookAuthor: String
    ): String {
        val payload = hashMapOf(
            "prompt" to prompt,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor
        )
        
        val result = functions
            .getHttpsCallable("detectIntent")
            .call(payload)
            .await()
            
        return (result.getData() as? Map<*, *>)?.get("response") as? String
            ?: throw Exception("Empty response from routing function")
    }
}
