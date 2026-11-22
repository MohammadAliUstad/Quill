package com.yugentech.quill.ui.access.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// The building blocks every onboarding scene (and the guest Aira screen's scene) is made of.

/**
 * A 0 → 1 entrance that starts [delayMs] after the scene becomes [active], on a soft spring
 * (it overshoots a touch, so things settle rather than stop).
 */
@Composable
internal fun rememberEntrance(active: Boolean, delayMs: Int): Animatable<Float, *> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            delay(delayMs.toLong())
            progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
        }
    }
    return progress
}

/** Fades in and rises [rise] into place as [progress] runs 0 → 1. */
internal fun Modifier.rise(progress: () -> Float, rise: Dp = 24.dp): Modifier = graphicsLayer {
    val t = progress()
    alpha = t.coerceIn(0f, 1f)
    translationY = (1f - t) * rise.toPx()
}

/** A scene's headline and the line under it, rising in after [delayMs]. */
@Composable
internal fun SceneText(title: String, body: String, active: Boolean, delayMs: Int) {
    val titleIn = rememberEntrance(active, delayMs)
    val bodyIn = rememberEntrance(active, delayMs + 140)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.rise({ titleIn.value })
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .rise({ bodyIn.value })
        )
    }
}

/** A hint with an icon: plain primary text straight on the aura, no container. */
@Composable
internal fun Hint(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = primary)
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = primary)
    }
}

/** Every scene's frame: centred, with room at the sides. */
@Composable
internal fun SceneColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}
