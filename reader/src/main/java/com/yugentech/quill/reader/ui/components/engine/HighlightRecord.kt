package com.yugentech.quill.reader.ui.components.engine

import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

data class HighlightRecord(
    val id: String,
    val bookId: String,
    val locatorJson: String,
    val colorInt: Int,
    val note: String? = null
) {
    fun getLocator(): Locator? {
        return try {
            Locator.fromJSON(JSONObject(locatorJson))
        } catch (e: Exception) {
            null
        }
    }
}