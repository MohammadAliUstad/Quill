package com.yugentech.quill.ui.dash.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- SCROLL-SYNCED BACKGROUND ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                }
                .background(
                    Brush.verticalGradient(
                        // Mimics a non-linear "ease-out" curve to completely eliminate the bottom seam
                        0.0f to surfaceColor.copy(alpha = 0.85f), // Lighter top so it's not too dark
                        0.4f to surfaceColor.copy(alpha = 0.65f),
                        0.7f to surfaceColor.copy(alpha = 0.30f), // Eases the fade
                        1.0f to surfaceColor.copy(alpha = 0.0f)   // Seamless invisible edge
                    )
                )
        )

        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior
        )
    }
}