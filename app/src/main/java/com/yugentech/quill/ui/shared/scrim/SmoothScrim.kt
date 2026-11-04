package com.yugentech.quill.ui.shared.scrim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private const val SCRIM_STEPS = 16

/**
 * Top-bar scrim that fades [color] from [topAlpha] to fully transparent along a smoothstep curve
 * (flat at both ends). A few hand-placed linear stops change slope abruptly, and each change
 * shows up as a visible band; sampling one smooth curve avoids that.
 *
 * [solidUntil] (0..1) holds [topAlpha] for that fraction of the height before the fade starts,
 * for headers whose controls need a near-opaque backing.
 */
fun smoothScrimBrush(color: Color, topAlpha: Float = 0.9f, solidUntil: Float = 0f): Brush =
    Brush.verticalGradient(
        colorStops = Array(SCRIM_STEPS + 1) { i ->
            val t = i / SCRIM_STEPS.toFloat()
            val f = ((t - solidUntil) / (1f - solidUntil)).coerceIn(0f, 1f)
            val eased = f * f * (3f - 2f * f)
            t to color.copy(alpha = topAlpha * (1f - eased))
        }
    )

@Composable
fun rememberSmoothScrimBrush(color: Color, topAlpha: Float = 0.9f, solidUntil: Float = 0f): Brush =
    remember(color, topAlpha, solidUntil) { smoothScrimBrush(color, topAlpha, solidUntil) }
