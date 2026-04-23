//package com.yugentech.quill.reader.ui.components.overlay.components.brightnessSlider.icon
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.unit.dp
//import kotlin.math.cos
//import kotlin.math.sin
//
//@Composable
//fun AnimatedSunIcon(
//    brightness: Float,
//    modifier: Modifier = Modifier,
//    color: Color = Color.Black
//) {
//    val rotationAngle = brightness * 90f
//
//    Canvas(
//        modifier = modifier
//            .size(24.dp)
//            .rotate(rotationAngle)
//    ) {
//        val center = Offset(size.width / 2, size.height / 2)
//        val coreRadius = size.width * 0.18f
//
//        val rayStrokeWidth = size.width * 0.08f
//        val rayStartGap = size.width * 0.32f
//
//        val maxRayEnd = size.width * 0.38f
//        val currentRayEnd = rayStartGap + ((maxRayEnd - rayStartGap) * brightness)
//
//        for (i in 0 until 8) {
//            val angleInDegrees = i * 45.0
//            val angleInRadians = Math.toRadians(angleInDegrees).toFloat()
//
//            val startX = center.x + cos(angleInRadians) * rayStartGap
//            val startY = center.y + sin(angleInRadians) * rayStartGap
//
//            val endX = center.x + cos(angleInRadians) * currentRayEnd
//            val endY = center.y + sin(angleInRadians) * currentRayEnd
//
//            drawLine(
//                color = color,
//                start = Offset(startX, startY),
//                end = Offset(endX, endY),
//                strokeWidth = rayStrokeWidth,
//                cap = StrokeCap.Round
//            )
//        }
//
//        drawCircle(
//            color = color,
//            radius = coreRadius,
//            center = center
//        )
//    }
//}