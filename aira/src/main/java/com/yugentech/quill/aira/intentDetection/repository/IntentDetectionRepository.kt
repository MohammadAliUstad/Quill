package com.yugentech.quill.aira.aira.intentDetection.repository

import com.yugentech.quill.aira.aira.intentDetection.model.Intent
import com.yugentech.quill.aira.aira.intentDetection.model.IntentResponse
import com.yugentech.quill.aira.aira.intentDetection.model.QueryIntent
import com.yugentech.quill.aira.aira.intentDetection.service.IntentDetectionService
import org.json.JSONObject
import timber.log.Timber

class IntentDetectionRepository(
    private val detectionService: IntentDetectionService
) {
    suspend fun detectIntent(
        question: String,
        title: String,
        author: String
    ): Intent {
        return try {
            val rawResponse = detectionService.getRoutingDecision(question, title, author)
            
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
                    intent = parseIntent(response.queryIntent)
                )
            } else {
                Intent.General
            }
        } catch (e: Exception) {
            Timber.e(e, "Intent detection failed, falling back to General")
            Intent.General
        }
    }

    private fun parseIntent(raw: String): QueryIntent = when (raw.lowercase().trim()) {
        "character_info" -> QueryIntent.CHARACTER_INFO
        "relationship" -> QueryIntent.RELATIONSHIP
        "plot_event" -> QueryIntent.PLOT_EVENT
        "quote_lookup" -> QueryIntent.QUOTE_LOOKUP
        else -> QueryIntent.GENERAL
    }
}
