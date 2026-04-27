package com.yugentech.quill.aira.aira.util

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class QueryIntent {
    CHARACTER_INFO,
    RELATIONSHIP,
    PLOT_EVENT,
    QUOTE_LOOKUP,
    GENERAL;

    val topPassages: Int
        get() = when (this) {
            CHARACTER_INFO -> 5
            RELATIONSHIP -> 5
            PLOT_EVENT -> 4
            QUOTE_LOOKUP -> 3
            GENERAL -> 4
        }

    val candidatesPerQuery: Int
        get() = when (this) {
            CHARACTER_INFO -> 40
            RELATIONSHIP -> 80
            PLOT_EVENT -> 30
            QUOTE_LOOKUP -> 20
            GENERAL -> 30
        }
}

sealed class QueryRoute {
    data class RagRequired(
        val queryVariations: List<String>,
        val entities: List<String>,
        val keywords: List<String>,
        val intent: QueryIntent
    ) : QueryRoute()

    object NoRagRequired : QueryRoute()
}

class QueryRouter(
    private val functions: FirebaseFunctions
) {

    suspend fun routeQuery(question: String, title: String, author: String): QueryRoute {
        return try {
            val prompt = AiraBuilder.buildRouterPrompt(question, title, author)
            val payload = hashMapOf("prompt" to prompt)

            val result = functions
                .getHttpsCallable("airaRoute")
                .call(payload)
                .await()

            val raw = (result.getData() as? Map<*, *>)?.get("response") as? String
                ?: return fallback(question)


            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)
            val isRAG = json.getBoolean("isRAG")

            if (isRAG) {
                val variations = parseStringArray(json.optJSONArray("queryVariations"))
                val entities = parseStringArray(json.optJSONArray("entities"))
                val keywords = parseStringArray(json.optJSONArray("keywords"))
                val intent = parseIntent(json.optString("queryIntent", "general"))

                if (variations.isEmpty()) return fallback(question)

                QueryRoute.RagRequired(
                    queryVariations = variations,
                    entities = entities,
                    keywords = keywords,
                    intent = intent
                )
            } else {
                QueryRoute.NoRagRequired
            }

        } catch (e: Exception) {
            fallback(question)
        }
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length())
            .map { array.getString(it).trim() }
            .filter { it.isNotBlank() }
    }

    private fun parseIntent(raw: String): QueryIntent = when (raw.lowercase().trim()) {
        "character_info" -> QueryIntent.CHARACTER_INFO
        "relationship" -> QueryIntent.RELATIONSHIP
        "plot_event" -> QueryIntent.PLOT_EVENT
        "quote_lookup" -> QueryIntent.QUOTE_LOOKUP
        else -> QueryIntent.GENERAL
    }

    private fun fallback(question: String): QueryRoute =
        QueryRoute.RagRequired(
            queryVariations = listOf(question),
            entities = emptyList(),
            keywords = emptyList(),
            intent = QueryIntent.GENERAL
        )
}