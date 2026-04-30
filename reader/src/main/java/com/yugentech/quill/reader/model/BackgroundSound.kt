package com.yugentech.quill.reader.model

import androidx.annotation.RawRes
import com.yugentech.quill.reader.R

// Enumeration mapping string IDs to raw audio resource files
enum class BackgroundSound(
    val id: String,
    val displayName: String,
    @param:RawRes val resId: Int?
) {
    NONE("none", "None", null),
    RAIN("rain", "Rain", R.raw.rain),
    BROWN_NOISE("brown_noise", "Brown Noise", R.raw.brown_noise),
    FIREPLACE("fireplace", "Fireplace", R.raw.fireplace),
    LIBRARY("library", "Library", R.raw.library),
    RIVERSIDE("riverside", "Riverside", R.raw.riverside),
    FOREST("forest", "Forest", R.raw.forest);

    companion object {
        // Helper to find a sound by its ID, returning NONE if not found
        fun fromId(id: String?): BackgroundSound {
            return entries.find { it.id == id } ?: NONE
        }
    }
}