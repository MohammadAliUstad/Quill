package com.yugentech.quill.ui.shared.bookDetailsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun ReadingProgressSection(
    book: Book,
    onContinueClick: () -> Unit
) {
    val timeAgo = remember(book.lastReadTime) {
        if (book.lastReadTime <= 0) return@remember "Not started"
        val now = Instant.now()
        val readTime = Instant.ofEpochMilli(book.lastReadTime)
        val zone = ZoneId.systemDefault()
        val diffMinutes = ChronoUnit.MINUTES.between(readTime, now)
        val diffHours = ChronoUnit.HOURS.between(readTime, now)
        val diffDays = ChronoUnit.DAYS.between(readTime.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate())

        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 && diffDays == 0L -> "${diffHours}h ago"
            diffDays == 1L -> "Yesterday"
            else -> "$diffDays days ago"
        }
    }

    val progressLabel = remember(book.progressPercent, book.totalPages) {
        if (book.totalPages > 0) {
            val currentPage = (book.progressPercent * book.totalPages).toInt()
            "Page $currentPage of ${book.totalPages}"
        } else {
            "${(book.progressPercent * 100).toInt()}%"
        }
    }

    Card(
        onClick = onContinueClick,
        shape = RoundedCornerShape(16.dp),
        // FIX 1: Styled to match the glassy look of the action buttons
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ROW 1: Header + Time (Aligned to ends)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Read",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (book.lastReadTime > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 2: Stats (Chapter + Page Count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                // FIX 2: Align to bottom so if the chapter title wraps,
                // the page count stays anchored neatly at the bottom baseline
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Current Chapter (allows text wrapping)
                StatItem(
                    icon = Icons.Default.Bookmark,
                    text = book.lastChapterTitle ?: "Chapter ${book.lastChapterIndex + 1}",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp) // Keep space between text and page count
                )

                // Right: Page Count
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    // FIX 3: Align to Top instead of Center. This ensures the icon stays next to
    // the first line of text even if the text wraps into 2 or 3 lines.
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(end = 6.dp, top = 2.dp) // Slight top padding to visually center with text line 1
                .size(16.dp) // Use fixed size instead of height
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // FIX 4: Removed maxLines and TextOverflow.Ellipsis so it naturally wraps
        )
    }
}