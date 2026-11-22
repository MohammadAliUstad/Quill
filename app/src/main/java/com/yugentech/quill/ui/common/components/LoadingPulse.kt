package com.yugentech.quill.ui.common.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.lerp

/** One full breath of a placeholder: from its resting tone to its brightest and back. */
private const val PULSE_MS = 1600L

/** How much later each placeholder breathes per screen width across, so a row breathes as a wave. */
private const val WAVE_DELAY_MS = 480L

/**
 * The standard loading placeholder: no moving highlight, just a calm pulse. Each placeholder
 * breathes between two surface tones (lifting toward a hint of the theme's primary at its
 * brightest), eased in and out. Placeholders further across the screen breathe a little later,
 * so a row of covers pulses as one soft wave from left to right. Every placeholder runs off the
 * same frame clock, so they stay in step.
 */
fun Modifier.loadingPulse(): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val resting = scheme.surfaceContainerHigh
    val bright = lerp(scheme.surfaceContainerHighest, scheme.primary, if (isDark) 0.08f else 0.05f)
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    var originX by remember { mutableFloatStateOf(0f) }
    var frameTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { frameTimeMs = it }
    }

    this
        .onGloballyPositioned { originX = it.positionInRoot().x }
        .drawBehind {
            // Where this placeholder sits across the screen sets how far behind the wave it runs
            val delay = (originX / screenWidth).coerceIn(0f, 1.5f) * WAVE_DELAY_MS
            val phase = ((frameTimeMs - delay) % PULSE_MS + PULSE_MS) % PULSE_MS / PULSE_MS
            // 0 → 1 → 0, eased at both ends
            val breath = (1f - cos(2f * PI.toFloat() * phase)) / 2f
            drawRect(lerp(resting, bright, breath))
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
                    .loadingPulse()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .loadingPulse()
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
                        .loadingPulse()
                )
            }
        }
    }
}

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
                        .loadingPulse()
                )
            }
        }
    }
}