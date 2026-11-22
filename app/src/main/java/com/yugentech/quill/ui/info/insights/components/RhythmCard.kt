package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.PartOfDay
import com.yugentech.quill.insights.engine.PartOfDayStat

/** Time read in each part of the day: the busiest part is filled, the rest sit quietly beside it. */
@Composable
fun RhythmCard(parts: List<PartOfDayStat>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val busiest = parts.filter { it.millis > 0 }.maxByOrNull { it.millis }?.part
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        parts.forEachIndexed { index, stat ->
            val isBusiest = stat.part == busiest
            val container = if (isBusiest) colors.primaryContainer else colors.surfaceContainerHigh
            val content = if (isBusiest) colors.onPrimaryContainer else colors.onSurface
            Surface(
                shape = rowItemShape(index, parts.size),
                color = container,
                contentColor = content,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(stat.part.icon(), contentDescription = null, modifier = Modifier.size(22.dp))
                    Text(
                        durationString(stat.millis),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        stringResource(stat.part.label()),
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun PartOfDay.icon(): ImageVector = when (this) {
    PartOfDay.MORNING -> Icons.Outlined.LightMode
    PartOfDay.AFTERNOON -> Icons.Outlined.WbSunny
    PartOfDay.EVENING -> Icons.Outlined.WbTwilight
    PartOfDay.NIGHT -> Icons.Outlined.ModeNight
}

private fun PartOfDay.label(): Int = when (this) {
    PartOfDay.MORNING -> R.string.insights_part_morning
    PartOfDay.AFTERNOON -> R.string.insights_part_afternoon
    PartOfDay.EVENING -> R.string.insights_part_evening
    PartOfDay.NIGHT -> R.string.insights_part_night
}
