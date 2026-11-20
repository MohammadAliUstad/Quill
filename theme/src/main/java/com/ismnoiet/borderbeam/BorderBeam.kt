//
//  BorderBeam.kt
//  BorderBeam
//
//  Author: ismnoiet
//  Minimum supported: Android 7.0 (API 24); AGSL shader on Android 13+ (API 33)
//
package com.ismnoiet.borderbeam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Container that draws a surface, hairline border and animated beam around ANY content:
 * text fields, menus, buttons, plain composables…
 *
 *     BorderBeam(shape = BeamShape.rounded(26), active = focused) {
 *         BasicTextField(text, { text = it })
 *     }
 *
 *     BorderBeam(BeamStyle.small, shape = BeamShape.circle) { StopIcon() }
 *     BorderBeam(BeamStyle.line, shape = BeamShape.capsule) { SearchField() }
 *     BorderBeam(colors = BeamPalette.single(Color.Cyan)) { ... }
 *     BorderBeam(direction = BeamDirection.rightToLeft) { ... }
 *
 * Same names, order and defaults as the iOS `BorderBeam`. There are two overloads so both the iOS
 * form (`style` first) and the Compose convention (`modifier` first) work: this one takes `style`
 * as its first, required argument; the other leaves `style` out and defaults it to
 * [BeamStyle.large].
 *
 * @param surface background fill, `null` for none
 * @param borderColor hairline border drawn inside the shape
 */
@Composable
fun BorderBeam(
    style: BeamStyle,
    modifier: Modifier = Modifier,
    shape: BeamShape = BeamShape.rounded(26),
    direction: BeamDirection = BeamDirection.leftToRight,
    active: Boolean = true,
    colors: List<Color> = BeamPalette.colorful,
    intensity: Double = 1.0,
    timing: BeamTiming = BeamTiming(),
    surface: Color? = Color(0.11f, 0.11f, 0.11f),
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable BoxScope.() -> Unit,
) {
    val outline = shape.toComposeShape()
    Box(
        modifier = modifier
            .then(if (surface != null) Modifier.background(surface, outline) else Modifier)
            .border(1.dp, borderColor, outline)
            .borderBeam(
                shape = shape,
                style = style,
                direction = direction,
                active = active,
                colors = colors,
                intensity = intensity,
                timing = timing,
            ),
        content = content,
    )
}

/** [BorderBeam] with the default [BeamStyle.large]. See the overload that takes `style`. */
@Composable
fun BorderBeam(
    modifier: Modifier = Modifier,
    shape: BeamShape = BeamShape.rounded(26),
    direction: BeamDirection = BeamDirection.leftToRight,
    active: Boolean = true,
    colors: List<Color> = BeamPalette.colorful,
    intensity: Double = 1.0,
    timing: BeamTiming = BeamTiming(),
    surface: Color? = Color(0.11f, 0.11f, 0.11f),
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable BoxScope.() -> Unit,
) {
    BorderBeam(
        style = BeamStyle.large,
        modifier = modifier,
        shape = shape,
        direction = direction,
        active = active,
        colors = colors,
        intensity = intensity,
        timing = timing,
        surface = surface,
        borderColor = borderColor,
        content = content,
    )
}

private fun BeamShape.toComposeShape(): Shape = when (this) {
    is Rounded -> RoundedCornerShape(radius)
    Capsule -> RoundedCornerShape(percent = 50)
    Circle -> CircleShape
}
