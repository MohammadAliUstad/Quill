//
//  BeamFallback.kt
//  BorderBeam
//
//  Author: ismnoiet
//  Minimum supported: Android 7.0 (API 24)
//
//  Canvas version of the beam for devices below Android 13, where AGSL (RuntimeShader) is not
//  available. It follows the same geometry and palette, but is an approximation: the beam is
//  drawn as short stroked segments along the border instead of per-pixel glow.
//
package com.ismnoiet.borderbeam

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val SEGMENTS = 36

internal fun DrawScope.drawBeamFallback(
    shape: BeamShape,
    style: BeamStyle,
    direction: BeamDirection,
    phase: Float,
    colors: List<Color>,
    intensity: Float,
    alpha: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val palette = fitPalette(colors)
    val r = min(shape.cornerRadius * density, min(w, h) / 2f)
    val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r))) }
    val strength = (intensity * alpha).coerceIn(0f, 1f)

    clipPath(clip) {
        if (style == BeamStyle.line) {
            drawSweep(phase, palette, strength)
        } else {
            drawOrbit(r, style, direction, phase, palette, strength)
        }
    }
}

private fun samplePalette(palette: List<Color>, t: Float): Color {
    if (palette.size == 1) return palette[0]
    val x = t.coerceIn(0f, 1f) * (palette.size - 1)
    val i = min(x.toInt(), palette.size - 2)
    return lerp(palette[i], palette[i + 1], x - i)
}

private fun bell(t: Float, power: Double): Float =
    Math.pow(max(sin(PI * t), 0.0), power).toFloat()

/** Comet circling the border: segments from tail (t = 0) to head (t = 1). */
private fun DrawScope.drawOrbit(
    r: Float,
    style: BeamStyle,
    direction: BeamDirection,
    phase: Float,
    palette: List<Color>,
    strength: Float,
) {
    val d = density
    val inset = 1f * d
    val path = Path().apply {
        addRoundRect(
            RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(max(r - inset, 0f)))
        )
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    if (length <= 0f) return

    val arc = if (style == BeamStyle.small) 0.5f else 0.36f
    val glowWidth = max(glowReach(style) * 0.9f, 6f) * d
    val segment = Path()

    for (k in 0 until SEGMENTS) {
        val t = (k + 0.5f) / SEGMENTS
        val color = samplePalette(palette, t)
        val weight = bell(t, 1.2) * color.alpha * strength
        if (weight <= 0.01f) continue

        val pos = (((1f - arc) + k * arc / SEGMENTS + direction.shaderValue * phase) % 1f + 1f) % 1f
        val s0 = pos * length
        val s1 = s0 + arc * length / SEGMENTS * 1.2f   // slight overlap so segments join up

        val pieces = if (s1 <= length) listOf(s0 to s1) else listOf(s0 to length, 0f to (s1 - length))
        for ((a, b) in pieces) {
            segment.rewind()
            measure.getSegment(a, b, segment, true)
            val rgb = color.copy(alpha = 1f)
            drawPath(segment, rgb.copy(alpha = 0.22f * weight), style = Stroke(glowWidth, cap = StrokeCap.Round))
            drawPath(segment, rgb.copy(alpha = 0.45f * weight), style = Stroke(4f * d, cap = StrokeCap.Round))
            drawPath(segment, rgb.copy(alpha = weight), style = Stroke(2f * d, cap = StrokeCap.Round))
        }
    }
}

/** Beam sweeping back and forth along the bottom edge (ease in/out). */
private fun DrawScope.drawSweep(phase: Float, palette: List<Color>, strength: Float) {
    val d = density
    val w = size.width
    val y = size.height - 1f * d
    val len = w * 0.28f
    val tri = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
    val eased = tri * tri * (3f - 2f * tri)
    val start = (w - len) * eased
    val glowWidth = max(glowReach(BeamStyle.line) * 0.9f, 6f) * d

    for (k in 0 until SEGMENTS) {
        val t = (k + 0.5f) / SEGMENTS
        val color = samplePalette(palette, t)
        val weight = bell(t, 1.2) * color.alpha * strength
        if (weight <= 0.01f) continue
        val x0 = start + len * k / SEGMENTS
        val x1 = start + len * (k + 1.2f) / SEGMENTS
        val rgb = color.copy(alpha = 1f)
        drawLine(rgb.copy(alpha = 0.22f * weight), Offset(x0, y), Offset(x1, y), glowWidth, StrokeCap.Round)
        drawLine(rgb.copy(alpha = 0.45f * weight), Offset(x0, y), Offset(x1, y), 4f * d, StrokeCap.Round)
        drawLine(rgb.copy(alpha = weight), Offset(x0, y), Offset(x1, y), 2f * d, StrokeCap.Round)
    }
}
