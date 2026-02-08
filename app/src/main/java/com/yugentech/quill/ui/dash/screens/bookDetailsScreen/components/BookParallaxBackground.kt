package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yugentech.quill.network.domain.Book

@Composable
fun BookParallaxBackground(
    book: Book,
    scrollState: ScrollState,
    headerHeight: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .graphicsLayer {
                // 1. Parallax: Move up at 50% speed of the actual scroll
                translationY = -scrollState.value * 0.5f

                // 2. Fade: Fade out as user scrolls past the header height
                val fadeStart = 0f
                val fadeEnd = size.height
                val currentAlpha = 1f - ((scrollState.value - fadeStart) / (fadeEnd - fadeStart))
                
                // Ensure alpha stays valid (0..1)
                alpha = currentAlpha.coerceIn(0f, 1f)
            }
    ) {
        // Blurred Background Image
        AsyncImage(
            model = book.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 3.dp)
                .alpha(0.7f)
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.8f to Color.Transparent,
                        1.0f to MaterialTheme.colorScheme.background
                    )
                )
        )
    }
}