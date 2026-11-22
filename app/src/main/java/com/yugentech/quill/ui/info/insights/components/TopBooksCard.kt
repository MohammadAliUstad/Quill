package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.BookTime
import com.yugentech.quill.ui.common.components.MaintenanceAwareCover
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

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
