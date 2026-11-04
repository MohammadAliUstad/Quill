package com.yugentech.quill.ui.config.appearance.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.ui.main.components.pressClickable
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import kotlin.math.PI
import kotlin.math.sin

private val TAU = (2 * PI).toFloat()

// Where things sit on the sheet, as fractions of it
private const val INKWELL_X = 0.78f
private const val NAME_TOP = 0.26f
// How the quill leans: resting in the inkwell, and lifted to write
private const val REST_LEAN = 26f
private const val WRITING_LEAN = 38f
// How far italic script runs past its measured width
private val INK_OVERHANG = 8.dp

/**
 * The colour themes as a grid of writing sheets, each in its theme's own colours with a quill
 * resting in an inkwell and the theme's name in ink. Choosing one lifts the quill out and it
 * writes the name afresh, the ink following its nib, before it settles at the end of the line.
 */
@Composable
fun InkwellTiles(
    themes: List<Pair<String, ColorScheme>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
    ) {
        themes.withIndex().chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                row.forEach { (index, theme) ->
                    InkwellTile(
                        name = theme.first,
                        scheme = theme.second,
                        isSelected = index == selectedIndex,
                        onClick = {
                            if (index != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                onSelect(index)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InkwellTile(
    name: String,
    scheme: ColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MaterialTheme.corners.large)
    // 0 = resting in the inkwell, 1 = lifted to the page
    val lift by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 160f),
        label = "quillLift"
    )
    // How much of the name is written; the chosen one is written again each time it's chosen
    val written = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            written.snapTo(0f)
            written.animateTo(1f, tween(durationMillis = 1100, delayMillis = 180, easing = FastOutSlowInEasing))
        }
    }
    val ink by animateColorAsState(
        if (isSelected) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.75f),
        label = "inkColour"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "sheetBorder"
    )
    val measurer = rememberTextMeasurer()
    val handwriting = TextStyle(
        fontFamily = FontFamily.Cursive,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    )
    val sheet = lerp(scheme.surfaceContainerLow, scheme.primaryContainer, 0.25f)

    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .pressClickable(shape = shape, pressedScale = 0.95f, onClick = onClick)
            .background(sheet)
            .border(2.5.dp, borderColor, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRules(scheme.primary)

            val text = measurer.measure(
                text = name,
                style = handwriting,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                constraints = Constraints(maxWidth = (size.width * 0.62f).toInt())
            )
            val textLeft = 12.dp.toPx()
            val textTop = size.height * NAME_TOP
            val progress = written.value
            // Italic script leans past its measured box, so the ink and the pen go a little further
            val reach = text.size.width + INK_OVERHANG.toPx()
            drawWriting(text, textLeft, textTop, reach, ink, progress)

            // Lifted, the nib rides along the end of the ink with a small scribble
            val penX = textLeft + reach * progress
            val penY = textTop + text.size.height * 0.8f + sin(progress * TAU * 4) * 2.dp.toPx() * (1 - progress)
            val rest = Offset(size.width * INKWELL_X, size.height * 0.62f)
            val nib = Offset(
                rest.x + (penX - rest.x) * lift,
                rest.y + (penY - rest.y) * lift
            )
            val lean = REST_LEAN + (WRITING_LEAN - REST_LEAN) * lift

            // Resting, the quill stands behind the inkwell's glass; lifted, it's in front
            if (lift < 0.5f) {
                drawQuill(scheme, nib, lean)
                drawInkwell(scheme)
            } else {
                drawInkwell(scheme)
                drawQuill(scheme, nib, lean)
            }
        }
    }
}

/** Faint ruled lines, like a sheet of writing paper. */
private fun DrawScope.drawRules(color: Color) {
    val gap = 14.dp.toPx()
    var y = size.height * 0.22f + gap
    while (y < size.height) {
        drawLine(color.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += gap
    }
}

/** The name in ink, as far as it's been written across [reach], with a flourish underneath. */
private fun DrawScope.drawWriting(
    text: TextLayoutResult,
    left: Float,
    top: Float,
    reach: Float,
    ink: Color,
    progress: Float
) {
    clipRect(right = left + reach * progress) {
        drawText(text, color = ink, topLeft = Offset(left, top))

        val y = top + text.size.height + 2.dp.toPx()
        val width = text.size.width.toFloat()
        val flourish = Path().apply {
            moveTo(left, y)
            quadraticTo(left + width * 0.35f, y + 4.dp.toPx(), left + width * 0.7f, y)
            quadraticTo(left + width * 0.9f, y - 3.dp.toPx(), left + width, y + 1.dp.toPx())
        }
        drawPath(flourish, ink.copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** A squat glass inkwell, dark with ink, with a shadow and a glint. */
private fun DrawScope.drawInkwell(scheme: ColorScheme) {
    val w = size.width
    val h = size.height
    val cx = w * INKWELL_X
    val bodyWidth = w * 0.24f
    val bodyHeight = h * 0.24f
    val bottom = h * 0.90f
    val glass = lerp(scheme.primary, Color.Black, 0.55f)

    drawOval(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(cx - bodyWidth * 0.6f, bottom - h * 0.03f),
        size = Size(bodyWidth * 1.2f, h * 0.07f)
    )
    // Neck, then the body over it
    val neckWidth = bodyWidth * 0.48f
    val neckTop = bottom - bodyHeight - h * 0.06f
    drawRoundRect(
        color = lerp(glass, Color.Black, 0.2f),
        topLeft = Offset(cx - neckWidth / 2, neckTop),
        size = Size(neckWidth, h * 0.08f),
        cornerRadius = CornerRadius(h * 0.015f)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(lerp(glass, Color.White, 0.18f), glass, lerp(glass, Color.Black, 0.3f)),
            startX = cx - bodyWidth / 2,
            endX = cx + bodyWidth / 2
        ),
        topLeft = Offset(cx - bodyWidth / 2, bottom - bodyHeight),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(bodyHeight * 0.3f)
    )
    // A glint on the glass
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(cx - bodyWidth * 0.30f, bottom - bodyHeight * 0.75f),
        end = Offset(cx - bodyWidth * 0.30f, bottom - bodyHeight * 0.35f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/** A feather quill with its [nib] at the given point, leaning [lean] degrees to the right. */
private fun DrawScope.drawQuill(scheme: ColorScheme, nib: Offset, lean: Float) {
    val length = size.height * 0.62f
    translate(nib.x, nib.y) {
        rotate(lean, pivot = Offset.Zero) {
            // The feather, drawn pointing straight up from the nib at the origin
            val vane = Path().apply {
                moveTo(0f, -length * 0.22f)
                quadraticTo(-length * 0.20f, -length * 0.58f, -length * 0.02f, -length)
                lineTo(0f, -length)
                quadraticTo(length * 0.13f, -length * 0.62f, 0f, -length * 0.22f)
                close()
            }
            drawPath(
                path = vane,
                brush = Brush.verticalGradient(
                    listOf(lerp(scheme.primary, Color.White, 0.25f), scheme.primary),
                    startY = -length,
                    endY = -length * 0.22f
                )
            )
            // Barbs across the vane
            val barb = lerp(scheme.primary, Color.White, 0.45f).copy(alpha = 0.6f)
            for (i in 1..6) {
                val y = -length * (0.30f + i * 0.10f)
                drawLine(barb, Offset(0f, y), Offset(-length * 0.12f, y - length * 0.06f), 0.8.dp.toPx())
            }
            // Shaft and nib
            drawLine(
                color = lerp(scheme.tertiary, Color.White, 0.30f),
                start = Offset.Zero,
                end = Offset(0f, -length),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            val nibPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(length * 0.025f, -length * 0.09f)
                lineTo(-length * 0.025f, -length * 0.09f)
                close()
            }
            drawPath(nibPath, lerp(scheme.tertiary, Color.Black, 0.35f))
        }
    }
}
