package com.yugentech.quill.ui.info.insights.components.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yugentech.quill.R
import com.yugentech.quill.localization.currentLocale
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The year in pixels, as in Sessions and Hajime: one cell per day for the last 52 weeks, shaded
 * by how long was read. Tapping a day shows its date and reading time in a chip above it.
 */
@Composable
fun Heatmap(
    intensities: Map<LocalDate, Int>,
    readByDay: Map<LocalDate, Long>,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val heatmapWeeks = remember(intensities, readByDay, locale) {
        generateHeatmapData(intensities, readByDay, locale)
    }
    val dayLetters = remember(locale) {
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) }
    }
    val listState = rememberLazyListState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val cellSize = 24.dp
    val cellSpacing = MaterialTheme.spacing.xs

    // Scrolling (the heatmap or the page) and leaving the screen dismiss the chip; it animates
    // out like any other dismissal, the popup following its cell while it fades
    val dismiss = { selectedDate = null }

    LaunchedEffect(Unit) {
        listState.scrollToItem(heatmapWeeks.size)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { dismiss() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { dismiss() }

    // The page scrolled (or its top bar collapsed): the card moved on screen
    val lastCardY = remember { floatArrayOf(Float.NaN) }

    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            delay(3_000)
            selectedDate = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val y = coordinates.positionInWindow().y
                if (!lastCardY[0].isNaN() && y != lastCardY[0]) dismiss()
                lastCardY[0] = y
            },
        shape = RoundedCornerShape(MaterialTheme.corners.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(MaterialTheme.spacing.m)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.heatmap_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = stringResource(R.string.heatmap_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(MaterialTheme.spacing.s))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                ) {
                    Text(text = stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall)
                    HeatmapCell(intensity = 1, size = cellSize)
                    HeatmapCell(intensity = 3, size = cellSize)
                    HeatmapCell(intensity = 5, size = cellSize)
                    Text(text = stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.m))

            Row {
                Column(
                    verticalArrangement = Arrangement.spacedBy(cellSpacing),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    dayLetters.forEach { letter ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(cellSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.width(MaterialTheme.spacing.s))

                LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    items(heatmapWeeks) { week ->
                        Column {
                            Box(modifier = Modifier.height(18.dp)) {
                                if (week.firstDayOfMonth != null) {
                                    Text(
                                        text = week.firstDayOfMonth,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            HeatmapWeekColumn(
                                days = week.days,
                                cellSize = cellSize,
                                spacing = cellSpacing,
                                selectedDate = selectedDate,
                                onDayClick = { day ->
                                    selectedDate = if (selectedDate == day.date) null else day.date
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapWeekColumn(
    days: List<HeatmapDay>,
    cellSize: Dp,
    spacing: Dp,
    selectedDate: LocalDate?,
    onDayClick: (HeatmapDay) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(7) { dayIndex ->
            val day = days.find { it.date.dayOfWeek.value == dayIndex + 1 }
            if (day != null) {
                Box {
                    HeatmapCell(
                        intensity = day.intensity,
                        size = cellSize,
                        dayOfMonth = day.date.dayOfMonth,
                        isSelected = day.date == selectedDate,
                        onClick = { onDayClick(day) }
                    )
                    // Always here, so it can animate out when the day is deselected
                    HeatmapDayTooltip(day = day, visible = day.date == selectedDate)
                }
            } else {
                HeatmapCell(intensity = -1, size = cellSize)
            }
        }
    }
}
