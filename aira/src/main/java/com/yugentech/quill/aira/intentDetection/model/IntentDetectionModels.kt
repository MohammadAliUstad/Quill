package com.yugentech.quill.aira.aira.intentDetection.model

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

sealed class Intent {
    data object General : Intent()
    
    data class BookRelated(
        val queryVariations: List<String>,
        val entities: List<String>,
        val keywords: List<String>,
        val intent: QueryIntent
    ) : Intent()
}

data class IntentResponse(
    val isRAG: Boolean,
    val queryVariations: List<String>,
    val entities: List<String>,
    val keywords: List<String>,
    val queryIntent: String
) {
    companion object {
        fun fromJson(json: JSONObject): IntentResponse {
            return IntentResponse(
                isRAG = json.optBoolean("isRAG", false),
                queryVariations = parseStringArray(json.optJSONArray("queryVariations")),
                entities = parseStringArray(json.optJSONArray("entities")),
                keywords = parseStringArray(json.optJSONArray("keywords")),
                queryIntent = json.optString("queryIntent", "general")
            )
        }

        private fun parseStringArray(array: JSONArray?): List<String> {
            if (array == null) return emptyList()
            return (0 until array.length())
                .map { array.getString(it).trim() }
                .filter { it.isNotBlank() }
        }
    }
}
