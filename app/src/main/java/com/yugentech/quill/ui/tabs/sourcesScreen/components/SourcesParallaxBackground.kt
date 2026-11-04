package com.yugentech.quill.ui.tabs.sourcesScreen.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Header backdrop for the Sources tab, built like LibraryParallaxBackground (half-speed parallax,
 * fades out over its own height, bottom blends into the surface) but with slowly drifting colour
 * blobs instead of a blurred cover.
 *
 * The import card's illustration is black line art. In dark theme the blobs use the light accent
 * tones rather than the dark containers, so the drawing still has a light backdrop to read against.
 */
@Composable
fun SourcesParallaxBackground(
    scrollOffset: Int,
    headerHeight: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface
    val isDark = surface.luminance() < 0.5f

    val primaryBlob = if (isDark) scheme.primary.copy(alpha = 0.55f) else scheme.primaryContainer
    val tertiaryBlob = if (isDark) scheme.tertiary.copy(alpha = 0.45f) else scheme.tertiaryContainer
    val secondaryBlob = if (isDark) scheme.secondary.copy(alpha = 0.4f) else scheme.secondaryContainer

    val transition = rememberInfiniteTransition(label = "sourcesBackdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 16_000, easing = LinearEasing)),
        label = "sourcesBackdropPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .graphicsLayer {
                translationY = -scrollOffset * 0.5f
                alpha = (1f - scrollOffset / size.height).coerceIn(0f, 1f)
                clip = true
            }
            .drawBehind {
                // Whole-number frequencies of one angle, so the loop closes seamlessly.
                val a = phase * 2f * PI.toFloat()
                val w = size.width
                val h = size.height

                drawBlob(
                    color = tertiaryBlob,
                    center = Offset(w * (0.18f + 0.10f * sin(a)), h * (0.22f + 0.08f * cos(a))),
                    radius = w * 0.7f
                )
                drawBlob(
                    color = secondaryBlob,
                    center = Offset(w * (0.45f + 0.15f * cos(a)), h * (0.6f + 0.06f * sin(2f * a))),
                    radius = w * 0.6f
                )
                // Sits behind the two illustrations on the import card (right two-thirds),
                // drifting only a little so the drawings always keep their backing.
                drawBlob(
                    color = primaryBlob,
                    center = Offset(w * (0.66f + 0.05f * sin(a + 1f)), h * (0.42f + 0.05f * cos(a))),
                    radius = w * 0.68f
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to surface.copy(alpha = 0.0f),
                        0.5f to surface.copy(alpha = 0.0f),
                        0.8f to surface.copy(alpha = 0.8f),
                        1.0f to surface
                    )
                )
        )
    }
}

private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
