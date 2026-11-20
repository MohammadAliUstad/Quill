//
//  BorderBeamModifier.kt
//  BorderBeam
//
//  Author: ismnoiet
//  Minimum supported: Android 7.0 (API 24); AGSL shader on Android 13+ (API 33)
//
package com.ismnoiet.borderbeam

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.random.Random

// MARK: - Speed (composition local)

/** Speed multiplier for every beam below this point (1 = normal, 2 = twice as fast). */
val LocalBeamSpeed = compositionLocalOf { 1.0 }

/**
 * Sets the speed of every `BorderBeam` / `Modifier.borderBeam` inside [content].
 *
 *     BeamSpeed(1.5) { Column { ... } }
 *
 * Changing it while a beam runs speeds it up or slows it down smoothly, without jumping.
 */
@Composable
fun BeamSpeed(speed: Double, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBeamSpeed provides speed, content = content)
}

/** True when the user turned animations off (the Android equivalent of Reduce Motion). */
private fun isReduceMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Adds an animated border beam to any composable.
 *
 *     MyView(Modifier.borderBeam(shape = BeamShape.capsule, style = BeamStyle.line))
 *     Modifier.borderBeam(BeamShape.rounded(12), colors = BeamPalette.single(Color.Cyan))
 *
 * Performance notes:
 * - one cheap GPU pass per beam on Android 13+, no blur layers
 * - the phase only invalidates drawing, never recomposition
 * - the frame loop stops while the beam is inactive (after the fade) and when animations are off
 */
fun Modifier.borderBeam(
    shape: BeamShape = BeamShape.rounded(26),
    style: BeamStyle = BeamStyle.large,
    direction: BeamDirection = BeamDirection.leftToRight,
    active: Boolean = true,
    colors: List<Color> = BeamPalette.colorful,
    intensity: Double = 1.0,
    timing: BeamTiming = BeamTiming(),
): Modifier = composed {
    val speed = LocalBeamSpeed.current
    val context = LocalContext.current
    val reduceMotion = remember(context) { isReduceMotionEnabled(context) }

    val fade by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween((timing.fade * 1000).toInt()),
        label = "borderBeamFade",
    )
    val running = (active || fade > 0.001f) && !reduceMotion

    // Phase is accumulated frame by frame, so the speed can change at any moment without a jump
    // (a plain time / duration would teleport the beam whenever the rate changes).
    val duration = if (style == BeamStyle.line) timing.lineDuration else timing.rotateDuration
    val rate by rememberUpdatedState((max(speed, 0.0) / max(duration, 0.01)).toFloat())
    var phase by remember { mutableFloatStateOf(if (reduceMotion) 0.15f else Random.nextFloat()) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)   // ignore long pauses
                    phase = (phase + dt * rate) % 1f
                }
                last = now
            }
        }
    }

    val palette = remember(colors) { packPalette(colors) }
    val agsl = remember { if (Build.VERSION.SDK_INT >= 33) AgslBeam() else null }

    drawWithContent {
        drawContent()
        if (fade > 0.001f) {
            if (Build.VERSION.SDK_INT >= 33 && agsl != null) {
                agsl.draw(this, shape, style, direction, phase, palette, intensity.toFloat(), fade)
            } else {
                drawBeamFallback(shape, style, direction, phase, colors, intensity.toFloat(), fade)
            }
        }
    }
}
