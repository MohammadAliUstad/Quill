package com.yugentech.quill.ui.config.appearance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.theme.LocalThemeReveal
import com.yugentech.theme.builder.getColorScheme
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.theme.tokens.spacing
import com.yugentech.quill.ui.main.components.SectionHeader

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

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            themeOptions.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    rowOptions.forEach { option ->
                        // Each tile is drawn in the scheme it would give, in the current mode
                        ThemeCard(
                            name = option.displayName,
                            scheme = getColorScheme(themeConfig.copy(colorTheme = option.colorTheme)),
                            isSelected = themeConfig.colorTheme == option.colorTheme,
                            onClick = {
                                // A simple fade from the old colours to the new, across the whole screen
                                val apply = { viewModel.updateTheme(themeConfig.copy(colorTheme = option.colorTheme)) }
                                reveal?.fade(apply, COLOR_THEME_FADE_MS) ?: apply()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // An odd last theme keeps its half width
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
