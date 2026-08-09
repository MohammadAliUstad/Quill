package com.yugentech.quill.reader.sound.model

import androidx.annotation.RawRes
import com.yugentech.quill.reader.R

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
        fun fromId(id: String?): BackgroundSound {
            return entries.find { it.id == id } ?: NONE
        }
    }
}