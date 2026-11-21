package com.yugentech.quill.ui.info.insights.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One day: -1 is outside the range (drawn empty), 0 no reading, 1–5 more reading. */
@Composable
fun HeatmapCell(
    intensity: Int,
    size: Dp = 24.dp,
    dayOfMonth: Int? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val backgroundColor = when {
        intensity == -1 -> Color.Transparent
        intensity == 0 -> colors.surfaceContainerHighest
        intensity < 2 -> colors.primary.copy(alpha = 0.25f)
        intensity < 4 -> colors.primary.copy(alpha = 0.55f)
        else -> colors.primary
    }
    val textColor = when {
        intensity == -1 -> Color.Transparent
        intensity == 0 -> colors.onSurfaceVariant.copy(alpha = 0.5f)
        intensity < 2 -> colors.primary
        intensity < 4 -> colors.onPrimaryContainer
        else -> colors.onPrimary
    }
    val shape = RoundedCornerShape(size * 0.25f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor)
            .then(if (isSelected) Modifier.border(2.dp, colors.onSurface, shape) else Modifier)
            .then(if (onClick != null && intensity != -1) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dayOfMonth != null && intensity != -1) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
