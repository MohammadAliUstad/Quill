//
//  BeamConfiguration.kt
//  BorderBeam
//
//  Author: ismnoiet
//  Minimum supported: Android 7.0 (API 24). The AGSL shader runs on Android 13+ (API 33);
//  older versions use a lighter Canvas fallback.
//
package com.ismnoiet.borderbeam

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Beam variants. */
enum class BeamStyle {
    /** Comet circling the whole border with a soft inner glow (cards, inputs). */
    large,

    /** Tighter, longer comet for small controls (icon buttons, avatars). */
    small,

    /** Glow sweeping back and forth along the bottom edge (search bars). */
    line,
}

/**
 * Outline the beam follows. Corner radii are circular so the beam hugs the edge exactly.
 *
 *     BeamShape.rounded(26)       // dp
 *     BeamShape.rounded(26.dp)
 *     BeamShape.capsule
 *     BeamShape.circle
 */
sealed interface BeamShape {
    companion object {
        /** Rounded rectangle. A plain number is read as dp (like points on iOS). */
        fun rounded(radius: Number): BeamShape = Rounded(radius.toFloat().dp)

        fun rounded(radius: Dp): BeamShape = Rounded(radius)

        val capsule: BeamShape get() = Capsule
        val circle: BeamShape get() = Circle
    }
}

internal data class Rounded(val radius: Dp) : BeamShape
internal data object Capsule : BeamShape
internal data object Circle : BeamShape

/** Corner radius in dp. Capsule and circle pass a huge value that the shader clamps to half the shortest side. */
internal val BeamShape.cornerRadius: Float
    get() = when (this) {
        is Rounded -> radius.value
        Capsule, Circle -> 10_000f
    }

/** Which way the beam travels when it circles the border (judged along the top edge). */
enum class BeamDirection(internal val shaderValue: Float) {
    /** Default. Clockwise: moves left to right along the top edge. */
    leftToRight(1f),

    /** Counter-clockwise: moves right to left along the top edge. */
    rightToLeft(-1f),
}

/**
 * Animation timings, in seconds. Override per instance with `timing =`.
 *
 * @param rotateDuration seconds for one full lap (orbit beams)
 * @param lineDuration seconds for one there-and-back sweep (edge beams)
 * @param fade fade in/out when `active` toggles
 */
data class BeamTiming(
    val rotateDuration: Double = 2.4,
    val lineDuration: Double = 2.0,
    val fade: Double = 0.6,
)

/** Ready-made color sets. Any `List<Color>` works too (first color = tail, last = head). */
object BeamPalette {
    val colorful: List<Color> = listOf(
        Color(0.25f, 0.85f, 0.45f),
        Color(0.20f, 0.60f, 0.95f),
        Color(0.70f, 0.35f, 0.95f),
        Color(1.00f, 0.35f, 0.65f),
        Color(1.00f, 0.65f, 0.25f),
    )

    val gemini: List<Color> = listOf(
        Color(0.26f, 0.52f, 0.96f),  // blue
        Color(0.61f, 0.45f, 0.80f),  // purple
        Color(0.92f, 0.26f, 0.21f),  // red
        Color(0.98f, 0.55f, 0.20f),  // orange
        Color(0.98f, 0.74f, 0.02f),  // yellow
        Color(0.20f, 0.66f, 0.33f),  // green
    )

    /** Blue -> teal -> cyan -> green -> yellow -> orange -> red (clockwise). */
    val spectrum: List<Color> = listOf(
        Color(0.26f, 0.50f, 0.96f),  // blue
        Color(0.25f, 0.70f, 0.90f),  // teal-blue
        Color(0.25f, 0.82f, 0.85f),  // cyan
        Color(0.25f, 0.78f, 0.40f),  // green
        Color(0.98f, 0.74f, 0.20f),  // yellow
        Color(0.98f, 0.52f, 0.22f),  // orange
        Color(0.92f, 0.28f, 0.25f),  // red
    )

    /** Cyan -> blue -> violet -> magenta. */
    val neon: List<Color> = listOf(
        Color(0.00f, 0.90f, 1.00f),
        Color(0.30f, 0.40f, 1.00f),
        Color(0.85f, 0.20f, 1.00f),
        Color(1.00f, 0.20f, 0.60f),
    )

    /** Gold -> orange -> coral -> purple. */
    val sunset: List<Color> = listOf(
        Color(1.00f, 0.78f, 0.30f),
        Color(1.00f, 0.50f, 0.30f),
        Color(1.00f, 0.30f, 0.45f),
        Color(0.70f, 0.30f, 0.90f),
    )

    /** Mint -> sky -> indigo -> lilac. */
    val aurora: List<Color> = listOf(
        Color(0.30f, 1.00f, 0.70f),
        Color(0.20f, 0.80f, 1.00f),
        Color(0.50f, 0.40f, 1.00f),
        Color(0.90f, 0.50f, 1.00f),
    )

    /** Cool, subtle white-blue. */
    val silver: List<Color> = listOf(
        Color(0.75f, 0.80f, 1.00f).copy(alpha = 0.2f),
        Color(0.90f, 0.93f, 1.00f),
        Color(0.75f, 0.80f, 1.00f).copy(alpha = 0.2f),
    )

    val mono: List<Color> = listOf(
        Color.White.copy(alpha = 0.25f),
        Color.White.copy(alpha = 0.9f),
        Color.White.copy(alpha = 0.25f),
    )

    /** One-color beam, softly fading in and out along its length. */
    fun single(color: Color): List<Color> =
        listOf(color.copy(alpha = 0.3f), color, color.copy(alpha = 0.3f))
}

internal const val MAX_BEAM_COLORS = 8

/** At most [max] colors; longer palettes are thinned evenly and keep their first and last color. */
internal fun fitPalette(colors: List<Color>, max: Int = MAX_BEAM_COLORS): List<Color> {
    val src = if (colors.isEmpty()) listOf(Color.White) else colors
    if (src.size <= max) return src
    return List(max) { i -> src[i * (src.size - 1) / (max - 1)] }
}

/** Palette flattened for the shader: [count] colors packed as RGBA floats, padded to [MAX_BEAM_COLORS]. */
internal class PackedPalette(val data: FloatArray, val count: Int)

internal fun packPalette(colors: List<Color>): PackedPalette {
    val fit = fitPalette(colors)
    val out = FloatArray(MAX_BEAM_COLORS * 4)
    fit.forEachIndexed { i, color ->
        val c = color.convert(ColorSpaces.Srgb)
        out[i * 4] = c.red
        out[i * 4 + 1] = c.green
        out[i * 4 + 2] = c.blue
        out[i * 4 + 3] = c.alpha
    }
    return PackedPalette(out, fit.size)
}
