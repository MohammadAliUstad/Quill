package com.yugentech.quill.reader.sound.model

import androidx.annotation.RawRes
import com.yugentech.quill.reader.R
import androidx.annotation.StringRes

enum class BackgroundSound(
    val id: String,
    @param:StringRes val displayName: Int,
    @param:RawRes val resId: Int?
) {
    NONE("none", R.string.reader_sound_none, null),
    RAIN("rain", R.string.reader_sound_rain, R.raw.rain),
    BROWN_NOISE("brown_noise", R.string.reader_sound_brown_noise, R.raw.brown_noise),
    FIREPLACE("fireplace", R.string.reader_sound_fireplace, R.raw.fireplace),
    LIBRARY("library", R.string.reader_sound_library, R.raw.library),
    RIVERSIDE("riverside", R.string.reader_sound_riverside, R.raw.riverside),
    FOREST("forest", R.string.reader_sound_forest, R.raw.forest);

    companion object {
        fun fromId(id: String?): BackgroundSound {
            return entries.find { it.id == id } ?: RAIN
        }
    }
}