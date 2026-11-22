package com.yugentech.quill.aira.intent.repository

import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.intent.model.IntentResponse
import com.yugentech.quill.aira.intent.model.QueryIntent
import com.yugentech.quill.aira.intent.service.IntentDetectionService
import com.yugentech.quill.aira.util.AiraLog
import org.json.JSONObject

class IntentDetectionRepository(
    private val detectionService: IntentDetectionService
) {
    suspend fun detectIntent(
        query: String,
        title: String,
        author: String,
        selectedText: String? = null,
        lastQuestion: String? = null,
        lastAnswer: String? = null
    ): Intent {
        return try {
            val rawResponse = detectionService.detectIntent(
                query, title, author, selectedText, lastQuestion, lastAnswer
            )

            val cleaned = rawResponse
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)
            val response = IntentResponse.fromJson(json)

            if (response.isRAG && response.queryVariations.isNotEmpty()) {
                Intent.BookRelated(
                    queryVariations = response.queryVariations,
                    entities = response.entities,
                    keywords = response.keywords,
                    intent = parseIntent(response.queryIntent),
                    characterName = response.characterName,
                    isChapterScoped = response.isChapterScoped
                )
            } else {
                Intent.General
            }
        } catch (e: Exception) {
            // Not fatal: the question still goes to general chat, just without the book's passages
            AiraLog.failure("DetectIntent", e)
            Intent.General
        }
    }

    private fun parseIntent(raw: String): QueryIntent = when (raw.lowercase().trim()) {
        "chapter_summary" -> QueryIntent.CHAPTER_SUMMARY
        "character_profile" -> QueryIntent.CHARACTER_PROFILE
        "character_recent" -> QueryIntent.CHARACTER_RECENT
        "character_arc" -> QueryIntent.CHARACTER_ARC
        "relationship" -> QueryIntent.RELATIONSHIP
        "plot_event" -> QueryIntent.PLOT_EVENT
        "theme_analysis" -> QueryIntent.THEME_ANALYSIS
        "significance" -> QueryIntent.SIGNIFICANCE
        "quote_lookup" -> QueryIntent.QUOTE_LOOKUP
        else -> QueryIntent.GENERAL
    }
}
