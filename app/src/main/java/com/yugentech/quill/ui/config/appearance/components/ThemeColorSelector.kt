package com.yugentech.quill.ui.config.appearance.components

import com.yugentech.theme.LocalThemeReveal
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.theme.builder.getColorScheme

@Composable
fun ThemeColorSelector(
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel
) {
    val themeConfig by viewModel.themeConfiguration.collectAsStateWithLifecycle()
    val reveal = LocalThemeReveal.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        SectionHeader(
            icon = Icons.Default.Palette,
            title = "Color Theme"
        )

        // Each card is drawn in the scheme its theme would give, in the current mode
        ThemePaletteTiles(
            themes = themeOptions.map { option ->
                option.displayName to getColorScheme(themeConfig.copy(colorTheme = option.colorTheme))
            },
            selectedIndex = themeOptions.indexOfFirst { it.colorTheme == themeConfig.colorTheme },
            onSelect = { index ->
                // A simple fade from the old colours to the new, across the whole screen
                val apply = { viewModel.updateTheme(themeConfig.copy(colorTheme = themeOptions[index].colorTheme)) }
                reveal?.fade(apply) ?: apply()
            }
        )
    }
}
