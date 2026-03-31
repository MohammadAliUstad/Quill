package com.yugentech.quill.ui.shared.bookDetailsScreen.components

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
import com.yugentech.quill.database.model.Book

@Composable
fun BookParallaxBackground(
    book: Book,
    scrollState: Int,
    headerHeight: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .graphicsLayer {
                // 1. Parallax: Move up at 50% speed of the actual scroll
                translationY = -scrollState * 0.5f

                // 2. Fade: Fade out as user scrolls past the header height
                val fadeStart = 0f
                val fadeEnd = size.height
                val currentAlpha = 1f - ((scrollState - fadeStart) / (fadeEnd - fadeStart))

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
                .blur(radius = 10.dp)
                .alpha(0.5f)
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.8f to MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        1.0f to MaterialTheme.colorScheme.background
                    )
                )
        )
    }
}