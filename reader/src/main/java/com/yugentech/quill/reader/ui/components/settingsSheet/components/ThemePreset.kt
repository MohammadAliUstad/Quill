package com.yugentech.quill.reader.ui.components.settingsSheet.components

import androidx.compose.ui.graphics.Color
import org.readium.r2.navigator.preferences.Theme
import androidx.annotation.StringRes

data class ThemePreset(
    @param:StringRes val name: Int,
    val theme: Theme,
    val bgColorInt: Int?,
    val textColorInt: Int?,
    val displayColor: Color,
    val isDarkBorder: Boolean = false
)