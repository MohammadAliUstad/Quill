package com.yugentech.quill.ui.config.appearance.components

import com.yugentech.theme.models.ColorTheme
import androidx.annotation.StringRes
import com.yugentech.quill.R

// The colour themes offered in Appearance; their colours come from the real schemes
data class ThemeOption(
    val colorTheme: ColorTheme,
    @param:StringRes val displayName: Int
)

val themeOptions = listOf(
    ThemeOption(ColorTheme.DYNAMIC, R.string.theme_name_dynamic),
    ThemeOption(ColorTheme.QUILL, R.string.theme_name_quill),
    ThemeOption(ColorTheme.LAGOON, R.string.theme_name_lagoon),
    ThemeOption(ColorTheme.CANYON, R.string.theme_name_canyon),
    ThemeOption(ColorTheme.HARVEST, R.string.theme_name_harvest),
    ThemeOption(ColorTheme.SAKURA, R.string.theme_name_sakura),
    ThemeOption(ColorTheme.ALPINE, R.string.theme_name_alpine),
    ThemeOption(ColorTheme.TWILIGHT, R.string.theme_name_twilight)
)
