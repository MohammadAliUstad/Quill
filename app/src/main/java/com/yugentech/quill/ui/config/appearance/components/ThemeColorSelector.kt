package com.yugentech.quill.ui.config.appearance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.theme.tokens.spacing

@Composable
fun ThemeColorSelector(
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel
) {
    val themeConfig by viewModel.themeConfiguration.collectAsStateWithLifecycle()
    val currentPrimary = MaterialTheme.colorScheme.primary
    val currentPrimaryContainer = MaterialTheme.colorScheme.primaryContainer

    val themeOptions = remember(currentPrimary, currentPrimaryContainer) {
        themeOptions(
            currentPrimary,
            currentPrimaryContainer
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        SectionHeader(
            icon = Icons.Default.Palette,
            title = ("Color Theme")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            themeOptions.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    rowOptions.forEach { themeOption ->
                        Box(modifier = Modifier.weight(1f)) {
                            ThemeCard(
                                themeOption = themeOption,
                                isSelected = themeConfig.colorTheme == themeOption.colorTheme,
                                onClick = {
                                    val newConfig =
                                        themeConfig.copy(colorTheme = themeOption.colorTheme)
                                    viewModel.updateTheme(newConfig)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}