package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val SHIMMER_SWEEP_MS = 1100L
private const val SHIMMER_PAUSE_MS = 500L

/**
 * The standard loading placeholder: a solid surface tone with a soft, lighter band sweeping
 * across it from left to right, then a short pause. The band is sized to the item, and every
 * placeholder runs off the same frame clock, so a row of them sweeps in step.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val base = scheme.surfaceContainerHigh
    val highlight = if (isDark) scheme.surfaceContainerHighest else scheme.surfaceContainerLowest

    var frameTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { frameTimeMs = it }
    }

    drawBehind {
        drawRect(base)
        val cycle = SHIMMER_SWEEP_MS + SHIMMER_PAUSE_MS
        val elapsed = frameTimeMs % cycle
        if (elapsed >= SHIMMER_SWEEP_MS) return@drawBehind
        val progress = FastOutSlowInEasing.transform(elapsed / SHIMMER_SWEEP_MS.toFloat())
        val bandWidth = size.width
        // The band travels from fully off the left edge to fully off the right edge
        val bandStart = -bandWidth + (size.width + bandWidth) * progress
        drawRect(
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                startX = bandStart,
                endX = bandStart + bandWidth
            )
        )
    }
}

@Composable
fun HeroCarouselSkeleton(title: String, subtitle: String) {
    // FIX 1: Match the 8.dp bottom padding of the real HeroCarousel
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically // Matched alignment to real dots
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(if (index == 0) 24.dp else 8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .shimmerEffect()
                )
            }
        }
    }
}

// 3. The Bookshelf Skeleton
@Composable
fun BookShelfSkeleton(title: String, subtitle: String) {
    // Match the 8.dp vertical padding of the real BookShelfRow
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            // FIX 2: Match the 24.dp horizontal padding and 16.dp spacing of the real BookShelfRow
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(4) {
                Box(
                    modifier = Modifier
                        // FIX 3: Match the 130.dp width of the real DiscoverBookCard
                        .width(130.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}