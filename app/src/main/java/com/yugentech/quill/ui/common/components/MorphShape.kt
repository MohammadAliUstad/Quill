package com.yugentech.quill.ui.common.components

import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round Material shapes that fill a circle about as fully as a cookie does, for a circle to bloom
 * into (the avatar sheet's selected avatar, the dock's + button). All symmetric, so a turn never
 * leaves one lopsided.
 */
val BloomShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Cookie7Sided,
    MaterialShapes.Cookie12Sided,
    MaterialShapes.Clover8Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Sunny
)

/**
 * The [morph] at [progress], turned [rotation] degrees about its centre and stretched over the
 * bounds. MaterialShapes are drawn in a 0..1 square, so each point is scaled up to the size.
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radians = Math.toRadians(rotation.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        fun x(px: Float, py: Float) = ((px - 0.5f) * c - (py - 0.5f) * s + 0.5f) * size.width
        fun y(px: Float, py: Float) = ((px - 0.5f) * s + (py - 0.5f) * c + 0.5f) * size.height

        val path = Path()
        morph.asCubics(progress).forEachIndexed { i, cubic ->
            if (i == 0) path.moveTo(x(cubic.anchor0X, cubic.anchor0Y), y(cubic.anchor0X, cubic.anchor0Y))
            path.cubicTo(
                x(cubic.control0X, cubic.control0Y), y(cubic.control0X, cubic.control0Y),
                x(cubic.control1X, cubic.control1Y), y(cubic.control1X, cubic.control1Y),
                x(cubic.anchor1X, cubic.anchor1Y), y(cubic.anchor1X, cubic.anchor1Y)
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}
