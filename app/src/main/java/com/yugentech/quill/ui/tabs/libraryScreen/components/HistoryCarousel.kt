package com.yugentech.quill.ui.tabs.libraryScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yugentech.quill.database.view.LibraryBookView
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCarousel(
    books: List<LibraryBookView>,
    onBookClick: (LibraryBookView) -> Unit,
    onCurrentBookChange: (LibraryBookView) -> Unit = {}
) {
    val carouselState = rememberCarouselState { books.size }

    LaunchedEffect(carouselState) {
        snapshotFlow { carouselState.currentItem }
            .collect { centerIndex ->
                if (books.isNotEmpty() && centerIndex >= 0 && centerIndex < books.size) {
                    onCurrentBookChange(books[centerIndex])
                }
            }
    }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier.fillMaxWidth(),
        preferredItemWidth = 280.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { index ->
        val book = books[index]
        HistoryCard(
            book = book,
            onClick = { onBookClick(book) },
            // FIX: Explicitly set the WIDTH here to prevent squishing
            modifier = Modifier
                .width(280.dp)  // <--- ADDED THIS (Forces the card to be wide)
                .height(130.dp) // Fixed Height
                .maskClip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun HistoryCard(
    book: LibraryBookView,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPage = remember(book.progressPercent, book.totalPages) {
        if (book.totalPages > 0) {
            ((book.progressPercent * (book.totalPages - 1)).roundToInt()).coerceIn(
                0,
                book.totalPages - 1
            ) + 1
        } else 0
    }

    val progressText = remember(currentPage, book.totalPages) {
        if (book.totalPages > 0) {
            "Page $currentPage of ${book.totalPages}"
        } else {
            "${(book.progressPercent * 100).toInt()}% Complete"
        }
    }

    val formattedDayInfo = remember(book.lastReadTime) {
        if (book.lastReadTime == 0L) return@remember "Not read yet"
        val zoneId = ZoneId.systemDefault()
        val lastReadDateTime =
            Instant.ofEpochMilli(book.lastReadTime).atZone(zoneId).toLocalDateTime()
        val now = LocalDateTime.now(zoneId)
        val daysBetween = ChronoUnit.DAYS.between(lastReadDateTime.toLocalDate(), now.toLocalDate())

        when {
            daysBetween <= 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween in 2..6 -> "$daysBetween days ago"
            else -> "$daysBetween days ago"
        }
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
            )

            // Content Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- TOP: Title ---
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = book.lastChapterTitle ?: "Continuing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // --- BOTTOM: Stats ---
                Column {
                    // Time Info
                    Text(
                        text = formattedDayInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Page Progress
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}