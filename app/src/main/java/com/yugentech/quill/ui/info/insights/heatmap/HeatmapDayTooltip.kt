package com.yugentech.quill.ui.info.insights.heatmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.yugentech.quill.R
import com.yugentech.quill.insghts.engine.InsightsEngine
import com.yugentech.quill.localization.asString
import com.yugentech.quill.localization.currentLocale
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The chip above a tapped heatmap day: the date and how long was read. Placed inside the cell's
 * layout so the popup anchors to it.
 *
 * Always composed with the cell: [visible] animates it in (fading and growing up from its bottom
 * edge, toward the cell) and out again, and the popup leaves only once it has finished hiding.
 */
@Composable
fun HeatmapDayTooltip(day: HeatmapDay, visible: Boolean) {
    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
    val positionProvider = remember(gap) { AboveAnchorPositionProvider(gap) }
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible

    // The popup stays while the chip is on screen or still animating out
    if (!visibility.currentState && !visibility.targetState) return

    val locale = currentLocale()
    Popup(popupPositionProvider = positionProvider) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(160)) +
                scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.85f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it / 6 },
            exit = fadeOut(tween(120)) +
                scaleOut(tween(150), targetScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                slideOutVertically(tween(150)) { it / 8 }
        ) {
            Surface(
                shape = RoundedCornerShape(MaterialTheme.corners.medium),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.s,
                        vertical = MaterialTheme.spacing.xs
                    )
                ) {
                    Text(
                        text = remember(day.date, locale) { formatTooltipDate(day.date, locale) },
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = when {
                            day.readMillis <= 0L -> stringResource(R.string.insights_no_reading)
                            day.readMillis < 60_000L -> stringResource(R.string.insights_under_minute)
                            else -> InsightsEngine.duration(day.readMillis).asString()
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTooltipDate(date: LocalDate, locale: Locale): String {
    val skeleton = if (date.year == LocalDate.now().year) "EEEMMMd" else "EEEMMMdyyyy"
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** Centres the popup over the anchor, within the window; below it if there's no room above. */
private class AboveAnchorPositionProvider(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxX)
        val above = anchorBounds.top - popupContentSize.height - gap
        val y = if (above >= 0) above else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}
