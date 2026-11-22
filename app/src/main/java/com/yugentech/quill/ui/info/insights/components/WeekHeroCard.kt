package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.DayActivity
import com.yugentech.quill.insights.engine.InsightsReport
import com.yugentech.quill.localization.currentLocale
import com.yugentech.theme.tokens.corners
import java.time.format.TextStyle

/**
 * The week at a glance on the primary container: time read against last week, days read, and a
 * bar for each of the last seven days that springs up when the screen opens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeekHeroCard(report: InsightsReport, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val content = colors.onPrimaryContainer
    val thisWeek = report.millisThisWeek
    val lastWeek = report.millisLastWeek
    val minute = 60_000L
    val (trendIcon, trendText) = when {
        thisWeek < minute && lastWeek < minute ->
            Icons.AutoMirrored.Outlined.TrendingFlat to stringResource(R.string.insights_trend_none)
        lastWeek < minute ->
            Icons.AutoMirrored.Outlined.TrendingUp to stringResource(R.string.insights_trend_fresh)
        thisWeek - lastWeek >= minute ->
            Icons.AutoMirrored.Outlined.TrendingUp to stringResource(R.string.insights_trend_more, durationString(thisWeek - lastWeek))
        lastWeek - thisWeek >= minute ->
            Icons.AutoMirrored.Outlined.TrendingDown to stringResource(R.string.insights_trend_less, durationString(lastWeek - thisWeek))
        else -> Icons.AutoMirrored.Outlined.TrendingFlat to stringResource(R.string.insights_trend_same)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.corners.extraLarge),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = content)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.insights_this_week),
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = 0.75f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = durationString(thisWeek),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                // Days read, on a soft-burst seal
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialShapes.SoftBurst.toShape())
                        .background(content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.insights_days_of_week, report.daysThisWeek),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.insights_days_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(trendIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = content.copy(alpha = 0.8f))
                Spacer(Modifier.width(6.dp))
                Text(trendText, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(18.dp))
            WeekBars(days = report.week, color = content)
        }
    }
}

@Composable
private fun WeekBars(days: List<DayActivity>, color: Color) {
    val locale = currentLocale()
    val most = days.maxOfOrNull { it.millis }?.coerceAtLeast(1L) ?: 1L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val growth = rememberGrowth(day.date, delayMs = 40L * index)
            val minutes = (day.millis / 60_000L).toInt()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (minutes > 0) {
                    Text(
                        "$minutes",
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = if (day.isToday) 1f else 0.7f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                }
                val fraction = day.millis.toFloat() / most
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // A day without reading still gets a dot, so the week reads as seven days
                        .height(if (minutes == 0) 8.dp else (8 + 62 * fraction).dp)
                        .graphicsLayer {
                            scaleY = growth.value.coerceAtLeast(0f)
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                day.isToday -> color
                                minutes == 0 -> color.copy(alpha = 0.15f)
                                else -> color.copy(alpha = 0.45f)
                            }
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = color.copy(alpha = if (day.isToday) 1f else 0.7f)
                )
            }
        }
    }
}
