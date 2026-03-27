package com.yugentech.quill.ui.more.insightsScreen.components.heatMap

import androidx.compose.foundation.background
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

@Composable
fun HeatmapCell(
    intensity: Int,
    size: Dp, // THE FIX: Added dynamic size parameter
    dayOfMonth: Int? = null
) {
    val backgroundColor = when {
        intensity == -1 -> Color.Transparent
        intensity == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
        intensity < 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        intensity < 4 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        intensity == -1 -> Color.Transparent
        intensity == 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        intensity < 2 -> MaterialTheme.colorScheme.primary
        intensity < 4 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = Modifier
            .size(size) // Updated to use dynamic size
            .clip(RoundedCornerShape(size * 0.25f)) // Keeps corners proportional (approx 6dp for 24dp size)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (dayOfMonth != null && intensity != -1) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                // Scales font size slightly based on cell size to maintain readability
                fontSize = 10.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}