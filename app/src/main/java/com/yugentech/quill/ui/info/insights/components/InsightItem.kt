package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.insights.engine.Insight
import com.yugentech.quill.insights.engine.InsightIcon
import com.yugentech.quill.insights.engine.InsightTone
import com.yugentech.quill.localization.asString

/**
 * One generated insight as a list item: its picture on an expressive shape in the tone's
 * colour, the headline and what it means. [shape] groups it with its neighbours.
 */
@Composable
fun InsightItem(insight: Insight, shape: Shape, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (insight.tone) {
        InsightTone.GOOD -> colors.primaryContainer to colors.onPrimaryContainer
        InsightTone.NEUTRAL -> colors.secondaryContainer to colors.onSecondaryContainer
        InsightTone.NUDGE -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(shape = shape, color = colors.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShapeBadge(insight.icon.vector(), insight.icon.shape(), container, content)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title.asString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = insight.body.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

private fun InsightIcon.vector(): ImageVector = when (this) {
    InsightIcon.STREAK -> Icons.Outlined.LocalFireDepartment
    InsightIcon.TREND_UP -> Icons.AutoMirrored.Outlined.TrendingUp
    InsightIcon.TREND_DOWN -> Icons.AutoMirrored.Outlined.TrendingDown
    InsightIcon.CLOCK -> Icons.Outlined.Schedule
    InsightIcon.CALENDAR -> Icons.Outlined.CalendarMonth
    InsightIcon.SPEED -> Icons.Outlined.Speed
    InsightIcon.DEEP -> Icons.Outlined.SelfImprovement
    InsightIcon.FINISH -> Icons.Outlined.Flag
    InsightIcon.BOOKMARK -> Icons.Outlined.Bookmark
    InsightIcon.SHELF -> Icons.Outlined.CollectionsBookmark
    InsightIcon.MILESTONE -> Icons.Outlined.EmojiEvents
    InsightIcon.HIGHLIGHT -> Icons.Outlined.BorderColor
    InsightIcon.NOTE -> Icons.Outlined.EditNote
    InsightIcon.SEEDLING -> Icons.Outlined.Eco
}

/** Each kind of insight on its own shape, so the list reads at a glance. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InsightIcon.shape(): Shape = when (this) {
    InsightIcon.STREAK, InsightIcon.MILESTONE -> MaterialShapes.Sunny
    InsightIcon.TREND_UP, InsightIcon.SPEED, InsightIcon.FINISH -> MaterialShapes.Arrow
    InsightIcon.TREND_DOWN, InsightIcon.BOOKMARK -> MaterialShapes.Cookie7Sided
    InsightIcon.CLOCK, InsightIcon.CALENDAR -> MaterialShapes.Cookie4Sided
    InsightIcon.DEEP, InsightIcon.SHELF -> MaterialShapes.Clover4Leaf
    InsightIcon.HIGHLIGHT, InsightIcon.NOTE -> MaterialShapes.Cookie9Sided
    InsightIcon.SEEDLING -> MaterialShapes.Flower
}.toShape()
