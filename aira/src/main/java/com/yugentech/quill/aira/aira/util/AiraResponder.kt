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
import org.json.JSONObject
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


        val contextBlock = AiraBuilder.buildContextBlock(chunks)
        val userPrompt = AiraBuilder.buildUserPrompt(question, contextBlock)
        val systemPrompt = AiraBuilder.buildRagSystemPrompt(book.title, book.author)
        val formattedHistory = formatHistory(history)
        val geminiRawResponse = callGemini(userPrompt, systemPrompt, formattedHistory)

        try {
            val cleanJson = geminiRawResponse
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()

            val startIndex = cleanJson.indexOf('{')
            val endIndex = cleanJson.lastIndexOf('}')

            if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                throw Exception("Valid JSON object not found in the response.")
            }

            val finalJsonString = cleanJson.substring(startIndex, endIndex + 1)
            val jsonObject = JSONObject(finalJsonString)

            val answerText = jsonObject.getString("answer")
            val usedIdsArray = jsonObject.optJSONArray("used_ids")

            val usedIds = mutableListOf<Int>()
            if (usedIdsArray != null) {
                for (i in 0 until usedIdsArray.length()) {
                    usedIds.add(usedIdsArray.getInt(i))
                }
            }

            val accurateSources = if (AiraBuilder.isDeadEnd(answerText)) {
                emptyList()
            } else {
                chunks.filterIndexed { index, _ -> index in usedIds }
            }

            emit(AiraResponse.Success(text = answerText, sources = accurateSources))

        } catch (e: Exception) {
            val fallbackSources =
                if (AiraBuilder.isDeadEnd(geminiRawResponse)) emptyList() else chunks
            emit(AiraResponse.Success(text = geminiRawResponse, sources = fallbackSources))
        }
    }

    fun respondGeneral(
        question: String,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        val systemPrompt = AiraBuilder.buildGeneralSystemPrompt(book.title, book.author)
        val formattedHistory = formatHistory(history)
        val rawResponse = callGemini(question, systemPrompt, formattedHistory)

        emit(AiraResponse.Success(text = rawResponse))
    }

    private suspend fun callGemini(
        prompt: String,
        systemPrompt: String,
        history: List<Map<String, Any>>
    ): String {
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

            (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: throw Exception("Empty response from function")

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