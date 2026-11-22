package com.yugentech.quill.ui.book.bookDetails.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.theme.tokens.dimensions.AppAnimations
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** What sits at the end of the Last Read header. */
private enum class LastReadTrailing { Deleted, Time, None }

/**
 * Where the reader left off, under the book's header: a small "Last read" label with when it
 * was, the chapter they're in as the card's main line, then a bar of how far through they
 * are, with the percentage beside it and the page underneath. Tapping it carries on reading,
 * while the book is on the device; once its download is removed the card says so and rests.
 *
 * Every piece of text that changes (the time, the chapter, the page, the percentage) fades
 * through to its new value rather than snapping, and the bar eases to the new progress.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReadingProgressSection(
    book: Book,
    onContinueClick: () -> Unit
) {
    val timeAgo = run {
        if (book.lastReadTime <= 0) return@run stringResource(R.string.not_started)
        val now = Instant.now()
        val readTime = Instant.ofEpochMilli(book.lastReadTime)
        val zone = ZoneId.systemDefault()
        val diffMinutes = ChronoUnit.MINUTES.between(readTime, now)
        val diffHours = ChronoUnit.HOURS.between(readTime, now)
        val diffDays = ChronoUnit.DAYS.between(readTime.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())

        when {
            diffMinutes < 1 -> stringResource(R.string.just_now)
            diffMinutes < 60 -> stringResource(R.string.minutes_ago, diffMinutes.toInt())
            diffHours < 24 && diffDays == 0L -> stringResource(R.string.hours_ago, diffHours.toInt())
            diffDays == 1L -> stringResource(R.string.yesterday)
            else -> pluralStringResource(R.plurals.days_ago, diffDays.toInt(), diffDays.toInt())
        }
    }

    val progress = book.progressPercent.coerceIn(0f, 1f)
    val pageLabel = if (book.totalPages > 0) {
        val currentPage = (progress * book.totalPages).toInt().coerceAtLeast(1)
        stringResource(R.string.page_of, currentPage, book.totalPages)
    } else {
        null
    }
    val percentLabel = "${(progress * 100).toInt()}%"
    val chapterLabel = book.lastChapterTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.chapter_number, book.lastChapterIndex + 1)

    val isDownloaded = book.downloadStatus == DownloadStatus.DOWNLOADED
    val colors = MaterialTheme.colorScheme
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "lastReadProgress"
    )

    Surface(
        onClick = onContinueClick,
        enabled = isDownloaded,
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            // "Last read" and when, or that the book's file is gone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.last_read),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.weight(1f)
                )

                // The last real time it was read, held while it fades away: deleting the book
                // also clears its last-read time, which would otherwise turn the fading label
                // into "Not started" on its way out to "Book deleted"
                var shownTimeAgo by remember { mutableStateOf(timeAgo) }
                if (book.lastReadTime > 0) shownTimeAgo = timeAgo
                val trailing = when {
                    !isDownloaded -> LastReadTrailing.Deleted
                    book.lastReadTime > 0 -> LastReadTrailing.Time
                    else -> LastReadTrailing.None
                }
                // A vertical swap, as the onboarding button's Continue and Start: the new label
                // pushes the old one out, upward on the way to "Book deleted" and downward when
                // the book comes back. Unclipped, so the longer label never gets sliced while the
                // width eases; the time itself fades through as it ticks on
                AnimatedContent(
                    targetState = trailing,
                    transitionSpec = {
                        val direction = if (targetState == LastReadTrailing.Deleted) 1 else -1
                        // A short hop (half the label's height) and quick, so it reads as a swap
                        (slideInVertically(tween(200)) { it / 2 * direction } + fadeIn(tween(200)))
                            .togetherWith(slideOutVertically(tween(200)) { -it / 2 * direction } + fadeOut(tween(150)))
                            .using(SizeTransform(clip = false) { _, _ -> tween(200) })
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "lastReadTrailing"
                ) { state ->
                    when (state) {
                        LastReadTrailing.Deleted -> Text(
                            text = stringResource(R.string.book_deleted),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.error
                        )

                        LastReadTrailing.Time -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(14.dp)
                            )
                            AnimatedContent(
                                targetState = shownTimeAgo,
                                transitionSpec = { fadeThrough() },
                                label = "lastReadTime"
                            ) { time ->
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        LastReadTrailing.None -> Unit
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // The chapter they're in, with an arrow while tapping it carries on reading
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = chapterLabel,
                    transitionSpec = { fadeThrough() },
                    contentAlignment = Alignment.CenterStart,
                    label = "lastReadChapter",
                    modifier = Modifier.weight(1f)
                ) { chapter ->
                    Text(
                        text = chapter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isDownloaded) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // How far through, as a bar with the percentage beside it
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (isDownloaded) colors.primary else colors.outline,
                    trackColor = colors.surfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                AnimatedContent(
                    targetState = percentLabel,
                    transitionSpec = { fadeThrough() },
                    contentAlignment = Alignment.CenterEnd,
                    label = "lastReadPercent"
                ) { percent ->
                    Text(
                        text = percent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (pageLabel != null) {
                Spacer(Modifier.height(6.dp))
                AnimatedContent(
                    targetState = pageLabel,
                    transitionSpec = { fadeThrough() },
                    label = "lastReadPage"
                ) { page ->
                    Text(
                        text = page,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
