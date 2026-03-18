package com.yugentech.quill.ui.more.appearanceScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.yugentech.quill.ui.mainScreen.components.SectionHeader
import com.yugentech.theme.AppFont
import com.yugentech.theme.ThemeViewModel
import com.yugentech.theme.builder.getFontFamily
import com.yugentech.theme.tokens.spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontSelector(
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel
) {
    val currentFont by viewModel.currentFont.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            icon = Icons.Default.TextFields,
            title = "App Font"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.xs)
        ) {
            items(AppFont.entries) { font ->
                FilterChip(
                    selected = currentFont == font,
                    onClick = { viewModel.setFont(font) },
                    label = {
                        Text(
                            text = font.displayName,
                            fontFamily = getFontFamily(font),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}