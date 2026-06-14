package com.yugentech.quill.aira.intent.repository

import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.intent.model.IntentResponse
import com.yugentech.quill.aira.intent.model.QueryIntent
import com.yugentech.quill.aira.intent.service.IntentDetectionService
import org.json.JSONObject
import timber.log.Timber

class IntentDetectionRepository(
    private val detectionService: IntentDetectionService
) {
    suspend fun detectIntent(
        query: String,
        title: String,
        author: String
    ): Intent {
        Timber.d("IntentDetection: Detecting intent for query='$query', book='$title'")
        return try {
            val rawResponse = detectionService.detectIntent(query, title, author)
            Timber.d("IntentDetection: Raw response from service: '$rawResponse'")
            
            val cleaned = rawResponse
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()
                
            val json = JSONObject(cleaned)
            val response = IntentResponse.fromJson(json)
            Timber.d("IntentDetection: Parsed response: $response")
            
            if (response.isRAG && response.queryVariations.isNotEmpty()) {
                val intent = Intent.BookRelated(
                    queryVariations = response.queryVariations,
                    entities = response.entities,
                    keywords = response.keywords,
                    intent = parseIntent(response.queryIntent)
                )
                Timber.d("IntentDetection: Decided on BookRelated: $intent")
                intent
            } else {
                Timber.d("IntentDetection: Decided on General")
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
