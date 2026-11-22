package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.HighlightStats
import com.yugentech.theme.tokens.spacing

/** Highlights, notes and this month's, the colours used most, and the most highlighted book. */
@Composable
fun HighlightsCard(stats: HighlightStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(
                stats.total to R.string.insights_highlights_label,
                stats.notes to R.string.insights_notes_label,
                stats.thisMonth to R.string.insights_this_month_label
            ).forEachIndexed { index, (count, label) ->
                val content = if (index == 0) colors.onSecondaryContainer else colors.onSurface
                Surface(
                    shape = rowItemShape(index, 3),
                    color = if (index == 0) colors.secondaryContainer else colors.surfaceContainerHigh,
                    contentColor = content,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(Modifier.padding(vertical = 12.dp, horizontal = 14.dp)) {
                        Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelMedium,
                            color = content.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // The colours as swatches, each with how often it's used
        if (stats.colors.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                stats.colors.forEach { (argb, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$count", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        if (stats.mostHighlightedTitle != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stats.mostHighlightedTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.insights_most_highlighted),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Text(
                    "${stats.mostHighlightedCount}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
