package com.yugentech.quill.ui.tabs.libraryScreen.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun LibraryParallaxBackground(
    coverUrl: String?,
    scrollOffset: Int, // CHANGED: Now takes an Int
    headerHeight: Dp
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .graphicsLayer {
                translationY = -scrollOffset * 0.5f

                val fadeStart = 0f
                val fadeEnd = size.height
                val currentAlpha = 1f - ((scrollOffset - fadeStart) / (fadeEnd - fadeStart))

                alpha = currentAlpha.coerceIn(0f, 1f)

                clip = true
            }
    ) {
        Crossfade(
            targetState = coverUrl,
            animationSpec = tween(durationMillis = 1500),
            label = "BackgroundCrossfade"
        ) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .alpha(0.5f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to bgColor.copy(alpha = 0.0f),
                        0.5f to bgColor.copy(alpha = 0.0f),
                        0.8f to bgColor.copy(alpha = 0.8f),
                        1.0f to bgColor
                    )
                )
        )
    }
}