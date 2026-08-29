package com.yugentech.quill.aira.chat.quickChat.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class VisualizeResult(
    val imageBase64: String,
    val mimeType: String
)

class VisualizeService(
    private val functions: FirebaseFunctions
) {
    suspend fun visualize(
        bookTitle: String,
        bookAuthor: String,
        passage: String
    ): VisualizeResult {
        val payload = hashMapOf(
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "passage" to passage
        )

        val callable = functions.getHttpsCallable("visualizeScene").apply {
            // Image generation runs meaningfully longer than a text quick action; the
            // default callable timeout is tight for that.
            setTimeout(120, TimeUnit.SECONDS)
        }

        val result = callable.call(payload).await()
        val data = result.getData() as? Map<*, *>
            ?: throw Exception("Empty response from visualize scene function")

        val imageBase64 = data["imageBase64"] as? String
            ?: throw Exception("No image returned from visualize scene function")
        val mimeType = data["mimeType"] as? String ?: "image/png"

        return VisualizeResult(imageBase64, mimeType)
    }
}
