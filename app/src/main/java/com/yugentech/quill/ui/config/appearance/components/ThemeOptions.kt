package com.yugentech.quill.ui.config.appearance.components

import com.yugentech.theme.models.ColorTheme

// The colour themes offered in Appearance; their colours come from the real schemes
data class ThemeOption(
    val colorTheme: ColorTheme,
    val displayName: String
)

val themeOptions = listOf(
    ThemeOption(ColorTheme.DYNAMIC, "Dynamic"),
    ThemeOption(ColorTheme.QUILL, "Quill"),
    ThemeOption(ColorTheme.LAGOON, "Lagoon"),
    ThemeOption(ColorTheme.CANYON, "Canyon"),
    ThemeOption(ColorTheme.HARVEST, "Harvest"),
    ThemeOption(ColorTheme.SAKURA, "Sakura"),
    ThemeOption(ColorTheme.ALPINE, "Alpine"),
    ThemeOption(ColorTheme.TWILIGHT, "Twilight")
)
