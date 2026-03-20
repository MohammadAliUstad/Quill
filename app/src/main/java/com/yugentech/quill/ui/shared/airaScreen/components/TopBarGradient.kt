package com.yugentech.quill.ui.shared.airaScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun TopBarGradient(modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to surfaceColor.copy(alpha = 1.0f),
                    0.3f to surfaceColor.copy(alpha = 0.95f),
                    0.6f to surfaceColor.copy(alpha = 0.75f),
                    0.85f to surfaceColor.copy(alpha = 0.40f),
                    1.0f to Color.Transparent
                )
            )
    )
}