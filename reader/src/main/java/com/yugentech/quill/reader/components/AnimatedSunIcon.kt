package com.yugentech.quill.reader.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSunIcon(
    brightness: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    // 1. Smooth Rotation
    // Rotating by 90 degrees ensures that at 1.0 brightness, the 8 rays
    // align perfectly back to their original symmetry, preventing visual snapping.
    val rotationAngle = brightness * 90f

    Canvas(
        modifier = modifier
            .size(24.dp)
            .rotate(rotationAngle)
    ) {
        val center = Offset(size.width / 2, size.height / 2)

        // 2. Tuned Proportions
        val coreRadius = size.width * 0.18f // Slightly smaller core
        val rayStrokeWidth = size.width * 0.08f

        // The fixed starting distance for all rays
        val rayStartGap = size.width * 0.32f

        // 3. The "Perfect Dot" Math
        // By making minRayEnd equal to rayStartGap, the line has a length of 0.
        // StrokeCap.Round will draw this zero-length line as a perfect circle!
        val minRayEnd = rayStartGap
        val maxRayEnd = size.width * 0.38f // Reduced maximum length for a tighter icon

        val currentRayEnd = minRayEnd + ((maxRayEnd - minRayEnd) * brightness)

        // 4. Draw the Rays FIRST
        // This ensures they render underneath the center core, completely
        // eliminating the "polygon/squashed" visual glitch.
        for (i in 0 until 8) {
            val angleInDegrees = i * 45.0
            val angleInRadians = Math.toRadians(angleInDegrees).toFloat()

            val startX = center.x + cos(angleInRadians) * rayStartGap
            val startY = center.y + sin(angleInRadians) * rayStartGap

            val endX = center.x + cos(angleInRadians) * currentRayEnd
            val endY = center.y + sin(angleInRadians) * currentRayEnd

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = rayStrokeWidth,
                cap = StrokeCap.Round
            )
        }

        // 5. Draw the Core SECOND
        // It sits perfectly on top.
        drawCircle(
            color = color,
            radius = coreRadius,
            center = center
        )
    }
}