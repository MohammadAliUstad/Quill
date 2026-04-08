package com.yugentech.quill.ui.more.appearanceScreen.components

import androidx.compose.ui.graphics.Color
import com.yugentech.theme.AppColorSchemes
import com.yugentech.theme.models.ColorTheme

data class ThemeOption(
    val colorTheme: ColorTheme,
    val displayName: String,
    val primaryColor: Color,
    val gradientColors: List<Color>
)

fun themeOptions(
    currentPrimary: Color,
    currentTertiary: Color
): List<ThemeOption> = listOf(

    // Dynamic
    ThemeOption(
        colorTheme = ColorTheme.DYNAMIC,
        displayName = "Dynamic",
        primaryColor = currentPrimary,
        gradientColors = listOf(
            currentPrimary,
            currentTertiary,
            currentPrimary.copy(alpha = 0.7f)
        )
    ),

    // Quill (Gold/Grey)
    ThemeOption(
        colorTheme = ColorTheme.QUILL,
        displayName = "Quill",
        primaryColor = AppColorSchemes.QuillLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.QuillLightColorScheme.primary,
            AppColorSchemes.QuillLightColorScheme.primaryContainer
        )
    ),

    // Canyon (Brown/Orange)
    ThemeOption(
        colorTheme = ColorTheme.CANYON,
        displayName = "Canyon",
        primaryColor = AppColorSchemes.CanyonLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.CanyonLightColorScheme.primary,
            AppColorSchemes.CanyonLightColorScheme.primaryContainer
        )
    ),

    // Harvest (Gold/Yellow)
    ThemeOption(
        colorTheme = ColorTheme.HARVEST,
        displayName = "Harvest",
        primaryColor = AppColorSchemes.HarvestLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.HarvestLightColorScheme.primary,
            AppColorSchemes.HarvestLightColorScheme.primaryContainer
        )
    ),

    // Grove (Olive/Yellow-Green)
    ThemeOption(
        colorTheme = ColorTheme.GROVE,
        displayName = "Grove",
        primaryColor = AppColorSchemes.GroveLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.GroveLightColorScheme.primary,
            AppColorSchemes.GroveLightColorScheme.primaryContainer
        )
    ),

    // Sakura (Pink/Red)
    ThemeOption(
        colorTheme = ColorTheme.SAKURA,
        displayName = "Sakura",
        primaryColor = AppColorSchemes.SakuraLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.SakuraLightColorScheme.primary,
            AppColorSchemes.SakuraLightColorScheme.primaryContainer
        )
    ),

    // Alpine (Deep Green/Forest)
    ThemeOption(
        colorTheme = ColorTheme.ALPINE,
        displayName = "Alpine",
        primaryColor = AppColorSchemes.AlpineLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.AlpineLightColorScheme.primary,
            AppColorSchemes.AlpineLightColorScheme.primaryContainer
        )
    ),

    // Twilight (Indigo/Purple tones)
    ThemeOption(
        colorTheme = ColorTheme.TWILIGHT,
        displayName = "Twilight",
        primaryColor = AppColorSchemes.TwilightLightColorScheme.primary,
        gradientColors = listOf(
            AppColorSchemes.TwilightLightColorScheme.primary,
            AppColorSchemes.TwilightLightColorScheme.primaryContainer
        )
    )
)