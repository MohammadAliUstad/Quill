package com.yugentech.quill.ui.info.insights.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

@Composable
fun GenreDistributionCard(
    genreDistribution: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val maxCount = genreDistribution.values.maxOrNull()?.toFloat() ?: 1f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
    ) {
        genreDistribution.entries.forEachIndexed { index, (genre, count) ->
            GenreRow(
                genre = genre,
                count = count,
                fraction = count / maxCount,
                animationDelay = index * 60
            )
        }
    }
}

@Composable
private fun GenreRow(
    genre: String,
    count: Int,
    fraction: Float,
    animationDelay: Int
) {
    var targetFraction by remember { mutableFloatStateOf(0f) }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 500, delayMillis = animationDelay),
        label = "genre_bar_$genre"
    )

    LaunchedEffect(fraction) { targetFraction = fraction }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = genre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (fraction == 1f) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count ${if (count == 1) "book" else "books"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(MaterialTheme.corners.extraLarge))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(MaterialTheme.corners.extraLarge))
                    .background(
                        if (fraction == 1f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
        }
    }
}