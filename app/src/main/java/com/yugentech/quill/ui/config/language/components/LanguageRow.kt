package com.yugentech.quill.ui.config.language.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yugentech.theme.shapes.itemShape
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One language, styled like Settings' rows (grouped shapes on the surface container). The
 * picked one takes the secondary container and a check in a filled circle ([SelectionMark]).
 */
@Composable
fun LanguageRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.surfaceContainer,
        tween(250),
        label = "languageRow"
    )
    val ring by animateColorAsState(
        if (selected) colors.primary else colors.outline,
        tween(250),
        label = "languageRing"
    )
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = MaterialTheme.spacing.s, top = MaterialTheme.spacing.s)
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.spacing.s, bottom = MaterialTheme.spacing.s)
            )
        },
        trailingContent = { SelectionMark(selected = selected, ring = ring) },
        modifier = Modifier
            // When the app switches language, names and subtitles in another script can need
            // more or less height; the row eases to its new size instead of jumping to it
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            )
            .clip(itemShape(index, count))
            .background(container)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * The radio mark at the end of a row. When a row is picked its circle springs full of the primary
 * colour (a touch past full, then settles), a soft ring ripples out from it, and the check draws
 * itself in as one stroke, short arm first. Unpicking runs it back quickly: the check retracts
 * and the circle drains. Rows that are already picked when the screen opens just show the mark.
 */
@Composable
private fun SelectionMark(selected: Boolean, ring: Color) {
    val colors = MaterialTheme.colorScheme
    val fill = remember { Animatable(if (selected) 1f else 0f) }
    val check = remember { Animatable(if (selected) 1f else 0f) }
    val pulse = remember { Animatable(1f) }
    // What the mark last showed, so the first composition (and a row scrolled back into view)
    // doesn't replay the animation
    val shown = remember { mutableStateOf(selected) }

    LaunchedEffect(selected) {
        if (shown.value == selected) return@LaunchedEffect
        shown.value = selected
        if (selected) {
            launch { fill.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
            launch {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
            delay(90)
            check.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        } else {
            launch { check.animateTo(0f, tween(120, easing = FastOutLinearInEasing)) }
            launch { pulse.snapTo(1f) }
            fill.animateTo(0f, tween(200, easing = FastOutLinearInEasing))
        }
    }

    val primary = colors.primary
    val onPrimary = colors.onPrimary
    Canvas(modifier = Modifier.size(24.dp)) {
        val radius = size.minDimension / 2f
        val ringWidth = 2.dp.toPx()

        // The ripple: grows past the mark and fades as it goes
        val p = pulse.value
        if (p < 1f) {
            drawCircle(
                color = primary.copy(alpha = 0.35f * (1f - p)),
                radius = radius * (1f + 0.7f * p),
                style = Stroke(width = ringWidth * (1f - 0.5f * p))
            )
        }

        drawCircle(color = ring, radius = radius - ringWidth / 2f, style = Stroke(width = ringWidth))

        val f = fill.value
        if (f > 0f) drawCircle(color = primary, radius = radius * f)

        val c = check.value
        if (c > 0f) {
            val path = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.67f)
                lineTo(size.width * 0.73f, size.height * 0.36f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * c, drawn, true)
            drawPath(
                path = drawn,
                color = onPrimary,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
