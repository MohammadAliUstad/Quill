package com.yugentech.quill.aira.aira.util

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AiraResponder(
    private val functions: FirebaseFunctions,
    private val ragRetriever: RagRetriever
) {

    fun respondWithRag(
        bookId: String,
        question: String,
        route: QueryRoute.RagRequired,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {

        val chunks = ragRetriever.retrieveWithExpansion(
            bookId = bookId,
            queries = route.queryVariations,
            entities = route.entities,
            boostedKeywords = (route.entities + route.keywords).distinct(),
            topPassages = route.intent.topPassages,
            candidatesPerQuery = route.intent.candidatesPerQuery,
            spoilerLockEnabled = book.spoilerLockEnabled
        )

        Timber.d("[AiraResponder] Intent: ${route.intent} | topPassages: ${route.intent.topPassages} | candidates: ${route.intent.candidatesPerQuery}")
        Timber.d("[AiraResponder] Retrieved ${chunks.size} chunks for ${route.queryVariations.size} variations")
        Timber.d("[AiraResponder] Chunks: ${chunks.map { "ch${it.chapterIndex}" }}")

        val contextBlock = AiraBuilder.buildContextBlock(chunks.map { it.text })
        val userPrompt = AiraBuilder.buildUserPrompt(question, contextBlock)
        val systemPrompt = AiraBuilder.buildRagSystemPrompt(book.title, book.author)
        val formattedHistory = formatHistory(history)

        emit(callGemini(userPrompt, systemPrompt, formattedHistory))
    }

    fun respondGeneral(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        val systemPrompt = AiraBuilder.buildGeneralSystemPrompt(book.title, book.author)
        val formattedHistory = formatHistory(history)

        emit(callGemini(question, systemPrompt, formattedHistory))
    }

    private suspend fun callGemini(
        prompt: String,
        systemPrompt: String,
        history: List<Map<String, Any>>
    ): AiraResponse {
        return try {
            val payload = hashMapOf(
                "prompt" to prompt,
                "systemPrompt" to systemPrompt,
                "history" to history
            )

            val result = functions
                .getHttpsCallable("airaChat")
                .call(payload)
                .await()

            val response = (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from function")

            AiraResponse.Success(response)

        } catch (e: Exception) {
            Timber.e(e, "AiraResponder callGemini failed")
            throw e
        }
    }

    private fun formatHistory(history: List<AiraMessageEntity>): List<Map<String, Any>> =
        history.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    AiraMessageRole.USER -> "user"
                    AiraMessageRole.AIRA -> "model"
                },
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }
}