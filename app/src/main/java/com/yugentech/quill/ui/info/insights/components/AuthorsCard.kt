package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.AuthorStat
import com.yugentech.theme.tokens.spacing

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
