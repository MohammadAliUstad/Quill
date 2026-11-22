package com.yugentech.quill.ui.info.insights.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.insights.engine.InsightsEngine
import com.yugentech.quill.localization.asString
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.delay

/** A section's title and the line under it. */
@Composable
fun InsightSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Stands in for a section with nothing to show yet. */
@Composable
fun EmptyInsightPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.components.controlBarItemWidthWide)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(MaterialTheme.corners.medium)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m)
        )
    }
}

/** An icon on one of Material 3 Expressive's shapes, in a container colour pair. */
@Composable
internal fun ShapeBadge(icon: ImageVector, shape: Shape, container: Color, content: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.48f))
    }
}

/** Grows from 0 to 1 with an expressive spring once it's composed, after [delayMs]. */
@Composable
internal fun rememberGrowth(key: Any?, delayMs: Long = 0): Animatable<Float, *> {
    val growth = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(delayMs)
        growth.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    return growth
}

/**
 * Horizontal counterpart of itemShape: a row of pieces reads as one grouped block, with large
 * corners only on the outer edges and small ones where neighbours meet.
 */
@Composable
fun rowItemShape(index: Int, count: Int): Shape {
    val large = MaterialTheme.corners.large
    val small = MaterialTheme.corners.small
    return when {
        count == 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(topStart = large, bottomStart = large, topEnd = small, bottomEnd = small)
        index == count - 1 -> RoundedCornerShape(topStart = small, bottomStart = small, topEnd = large, bottomEnd = large)
        else -> RoundedCornerShape(small)
    }
}

@Composable
internal fun durationString(millis: Long): String = InsightsEngine.duration(millis).asString()
