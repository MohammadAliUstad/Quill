package com.yugentech.quill.aira.intent.model

import org.json.JSONArray
import org.json.JSONObject

enum class QueryIntent {
    CHAPTER_SUMMARY,
    CHARACTER_PROFILE,
    CHARACTER_RECENT,
    CHARACTER_ARC,
    RELATIONSHIP,
    PLOT_EVENT,
    THEME_ANALYSIS,
    SIGNIFICANCE,
    QUOTE_LOOKUP,
    GENERAL;

    // Only consulted by the generic multi-query retrieval path -- CHAPTER_SUMMARY and the three
    // character-specific intents use their own specialized retrieval functions instead, each
    // with its own tuned budget, and never read these.
    val topPassages: Int
        get() = when (this) {
            RELATIONSHIP -> 10
            // A "whodunit"-shaped question can require surfacing a confession or reveal that's
            // nowhere near the initial event in the book, and may not be the top semantic match
            // for the obvious phrasing of the question -- a bigger net matters more here than
            // anywhere else on the generic path.
            PLOT_EVENT -> 18
            QUOTE_LOOKUP -> 6
            else -> 8
        }

    val candidatesPerQuery: Int
        get() = when (this) {
            RELATIONSHIP -> 100
            PLOT_EVENT -> 90
            QUOTE_LOOKUP -> 40
            else -> 50
        }
}

sealed class Intent {
    data object General : Intent()

    data class BookRelated(
        val queryVariations: List<String>,
        val entities: List<String>,
        val keywords: List<String>,
        val intent: QueryIntent,
        // Populated only for CHARACTER_PROFILE / CHARACTER_RECENT / CHARACTER_ARC.
        val characterName: String? = null,
        // Populated only for THEME_ANALYSIS / SIGNIFICANCE -- true means "about the current
        // chapter specifically", false means "about the book as a whole".
        val isChapterScoped: Boolean = false
    ) : Intent()
}

data class IntentResponse(
    val isRAG: Boolean,
    val queryVariations: List<String>,
    val entities: List<String>,
    val keywords: List<String>,
    val queryIntent: String,
    val characterName: String? = null,
    val isChapterScoped: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): IntentResponse {
            return IntentResponse(
                isRAG = json.optBoolean("isRAG", false),
                queryVariations = parseStringArray(json.optJSONArray("queryVariations")),
                entities = parseStringArray(json.optJSONArray("entities")),
                keywords = parseStringArray(json.optJSONArray("keywords")),
                queryIntent = json.optString("queryIntent", "general"),
                characterName = json.optString("characterName", "").trim().takeIf { it.isNotBlank() },
                isChapterScoped = json.optBoolean("isChapterScoped", false)
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
