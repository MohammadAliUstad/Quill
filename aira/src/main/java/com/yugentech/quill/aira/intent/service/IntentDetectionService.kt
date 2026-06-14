package com.yugentech.quill.aira.intent.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class IntentDetectionService(
    private val functions: FirebaseFunctions
) {
    suspend fun detectIntent(
        query: String,
        bookTitle: String,
        bookAuthor: String
    ): String {
        Timber.d("IntentDetectionService: Calling 'detectIntent' for query: '$query'")
        val payload = hashMapOf(
            "query" to query,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor
        )
        
        return try {
            val result = functions
                .getHttpsCallable("detectIntent")
                .call(payload)
                .await()
            
            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
            if (response == null) {
                Timber.e("IntentDetectionService: Function returned null or invalid data")
                throw Exception("Empty response from routing function")
            }

            Timber.d("IntentDetectionService: Successfully received response: $response")
            response
        } catch (e: Exception) {
            Timber.e(e, "IntentDetectionService: Function call failed")
            throw e
        }
    }
}