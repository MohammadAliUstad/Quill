package com.yugentech.quill.ui.info.insights.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insghts.engine.AiraStats
import com.yugentech.quill.insghts.engine.AuthorStat
import com.yugentech.quill.insghts.engine.BookTime
import com.yugentech.quill.insghts.engine.DayActivity
import com.yugentech.quill.insghts.engine.HighlightStats
import com.yugentech.quill.insghts.engine.Insight
import com.yugentech.quill.insghts.engine.InsightIcon
import com.yugentech.quill.insghts.engine.InsightTone
import com.yugentech.quill.insghts.engine.InsightsEngine
import com.yugentech.quill.insghts.engine.InsightsReport
import com.yugentech.quill.insghts.engine.LibraryStats
import com.yugentech.quill.insghts.engine.PartOfDay
import com.yugentech.quill.insghts.engine.PartOfDayStat
import com.yugentech.quill.insghts.engine.ReadingRecords
import com.yugentech.quill.localization.asString
import com.yugentech.quill.localization.currentLocale
import com.yugentech.quill.ui.shared.maintenance.MaintenanceAwareCover
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

// ---------------------------------------------------------------- Shared bits

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
private fun ShapeBadge(icon: ImageVector, shape: Shape, container: Color, content: Color, size: Dp = 44.dp) {
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
private fun rememberGrowth(key: Any?, delayMs: Long = 0): Animatable<Float, *> {
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
private fun durationString(millis: Long): String = InsightsEngine.duration(millis).asString()

// ---------------------------------------------------------------- This week

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

// ---------------------------------------------------------------- Glance stats

/**
 * Total time, streak and books finished as one grouped row: round outer corners, small ones
 * where they meet, each in its own container colour with its icon on an expressive shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlanceRow(report: InsightsReport, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        GlanceTile(
            icon = Icons.Outlined.Schedule,
            badge = MaterialShapes.Cookie6Sided.toShape(),
            value = durationString(report.totalReadMillis),
            label = stringResource(R.string.insights_glance_time),
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            shape = rowItemShape(0, 3),
            modifier = Modifier.weight(1f)
        )
        GlanceTile(
            icon = Icons.Outlined.LocalFireDepartment,
            badge = MaterialShapes.Sunny.toShape(),
            value = "${report.streak}",
            label = stringResource(R.string.insights_glance_streak),
            container = colors.tertiaryContainer,
            content = colors.onTertiaryContainer,
            shape = rowItemShape(1, 3),
            modifier = Modifier.weight(1f)
        )
        GlanceTile(
            icon = Icons.Outlined.CheckCircle,
            badge = MaterialShapes.Clover4Leaf.toShape(),
            value = "${report.finishedBooks}",
            label = stringResource(R.string.insights_glance_finished),
            container = colors.surfaceContainerHigh,
            content = colors.onSurface,
            shape = rowItemShape(2, 3),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlanceTile(
    icon: ImageVector,
    badge: Shape,
    value: String,
    label: String,
    container: Color,
    content: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(shape = shape, color = container, contentColor = content, modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShapeBadge(icon, badge, content.copy(alpha = 0.12f), content, size = 36.dp)
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Generated insights

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

// ---------------------------------------------------------------- Rhythm

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

// ---------------------------------------------------------------- Personal bests

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

// ---------------------------------------------------------------- Most read

/** The books with the most time spent reading them: cover, time read and how far along. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopBooksCard(books: List<BookTime>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
        books.forEachIndexed { index, book ->
            val growth = rememberGrowth(book.id, delayMs = 60L * index)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MaintenanceAwareCover(
                    url = book.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    iconSize = 20.dp,
                    modifier = Modifier
                        .size(width = 48.dp, height = 70.dp)
                        .clip(RoundedCornerShape(MaterialTheme.corners.small))
                        .background(colors.surfaceContainerHighest)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.author.isNotBlank()) {
                        Text(
                            book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearWavyProgressIndicator(
                        progress = { book.progress.coerceIn(0f, 1f) * growth.value.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHighest
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = durationString(book.millis) + " · " +
                            stringResource(R.string.insights_percent_read, (book.progress * 100).toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Authors

/** Authors ranked by the time spent with their books. */
@Composable
fun AuthorsCard(authors: List<AuthorStat>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)) {
        authors.forEachIndexed { index, author ->
            val rankColor = when (index) {
                0 -> colors.primary
                1 -> colors.secondary
                else -> colors.onSurfaceVariant
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = if (index == 0) 0.15f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = rankColor)
                }
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    if (author.millis >= 60_000L) {
                        Text(durationString(author.millis), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        pluralStringResource(R.plurals.book_count, author.books, author.books),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Library

/** Where the library stands, as one segmented bar with a count under each part. */
@Composable
fun LibraryCard(library: LibraryStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val segments = listOf(
        Triple(R.string.insights_status_finished, library.finished, colors.primary),
        Triple(R.string.insights_status_reading, library.inProgress, colors.secondary),
        Triple(R.string.insights_status_not_started, library.notStarted, colors.outlineVariant)
    )
    val total = library.total.coerceAtLeast(1).toFloat()
    val growth = rememberGrowth(library)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .graphicsLayer {
                    scaleX = growth.value.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val shown = segments.filter { it.second > 0 }
            shown.forEachIndexed { index, (_, count, color) ->
                Box(
                    Modifier
                        .weight(count / total)
                        .fillMaxHeight()
                        .clip(rowItemShape(index, shown.size))
                        .background(color)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            segments.forEach { (label, count, color) ->
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Highlights

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

// ---------------------------------------------------------------- Aira

/** Reading with Aira: questions asked, the book explored most, and visuals created. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiraCard(aira: AiraStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
        EngagementRow(
            icon = Icons.Outlined.AutoAwesome,
            badge = MaterialShapes.Cookie9Sided.toShape(),
            container = colors.primaryContainer,
            content = colors.onPrimaryContainer,
            title = if (aira.questions == 0) stringResource(R.string.no_questions_yet)
            else pluralStringResource(R.plurals.questions_asked, aira.questions, aira.questions),
            subtitle = stringResource(R.string.total_conversations)
        )
        if (aira.mostExploredTitle != null) {
            EngagementRow(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                badge = MaterialShapes.Clover4Leaf.toShape(),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                title = aira.mostExploredTitle,
                subtitle = stringResource(R.string.most_explored)
            )
        }
        if (aira.visuals > 0) {
            EngagementRow(
                icon = Icons.Outlined.Image,
                badge = MaterialShapes.Flower.toShape(),
                container = colors.tertiaryContainer,
                content = colors.onTertiaryContainer,
                title = pluralStringResource(R.plurals.insights_visuals_created, aira.visuals, aira.visuals),
                subtitle = stringResource(R.string.visuals_subtitle)
            )
        }
    }
}

@Composable
private fun EngagementRow(icon: ImageVector, badge: Shape, container: Color, content: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {
        ShapeBadge(icon, badge, container, content, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
