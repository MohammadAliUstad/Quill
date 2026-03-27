package com.yugentech.quill.ui.more.insightsScreen.components.heatMap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.more.insightsScreen.insights.HeatmapDay

@Composable
fun HeatmapWeekColumn(
    days: List<HeatmapDay>,
    cellSize: Dp // THE FIX: Added dynamic size parameter
) {
    Column(
        // Match this spacing (e.g., 4.dp or MaterialTheme.spacing.xs)
        // to the horizontal spacing in Heatmap.kt for a perfect grid.
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(7) { dayIndex ->
            val day = days.find { it.date.dayOfWeek.value == dayIndex + 1 }

            if (day != null) {
                HeatmapCell(
                    intensity = day.intensity,
                    size = cellSize, // Passed to cell
                    dayOfMonth = day.date.dayOfMonth
                )
            } else {
                // Important: Even empty spacer cells need the size to keep alignment
                HeatmapCell(
                    intensity = -1,
                    size = cellSize
                )
            }
        }
    }
}