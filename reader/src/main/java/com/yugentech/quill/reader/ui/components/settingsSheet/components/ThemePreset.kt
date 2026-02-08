package com.yugentech.quill.reader.ui.settingsSheet.components

import androidx.compose.ui.graphics.Color
import org.readium.r2.navigator.preferences.Theme

data class ThemePreset(
    val name: String,
    val theme: Theme,
    val bgColorInt: Int?,
    val textColorInt: Int?,
    val displayColor: Color,
    val isDarkBorder: Boolean = false
)