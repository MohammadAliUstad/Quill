package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.ReadingRecords
import com.yugentech.quill.localization.currentLocale
import com.yugentech.theme.tokens.corners
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The reading records as a grouped two-by-two grid. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecordsCard(records: ReadingRecords, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val bestDayLabel = records.bestDay?.let {
        remember(it, locale) { it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
    }
    val large = MaterialTheme.corners.large
    val small = MaterialTheme.corners.small
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            RecordTile(
                icon = Icons.Outlined.Schedule,
                value = durationString(records.longestSessionMillis),
                label = stringResource(R.string.insights_best_session),
                shape = RoundedCornerShape(topStart = large, topEnd = small, bottomStart = small, bottomEnd = small),
                modifier = Modifier.weight(1f)
            )
            RecordTile(
                icon = Icons.Outlined.Speed,
                value = durationString(records.averageSessionMillis),
                label = stringResource(R.string.insights_avg_session),
                shape = RoundedCornerShape(topStart = small, topEnd = large, bottomStart = small, bottomEnd = small),
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            RecordTile(
                icon = Icons.Outlined.CalendarMonth,
                value = durationString(records.bestDayMillis),
                label = stringResource(R.string.insights_best_day),
                detail = bestDayLabel,
                shape = RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = small),
                modifier = Modifier.weight(1f)
            )
            RecordTile(
                icon = Icons.Outlined.LocalFireDepartment,
                value = pluralStringResource(R.plurals.profile_streak_days, records.bestStreak, records.bestStreak),
                label = stringResource(R.string.insights_best_streak),
                shape = RoundedCornerShape(topStart = small, topEnd = small, bottomStart = small, bottomEnd = large),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecordTile(
    icon: ImageVector,
    value: String,
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = shape, color = colors.surfaceContainerHigh, modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(alpha = 0.75f), maxLines = 1)
            }
        }
    }
}
