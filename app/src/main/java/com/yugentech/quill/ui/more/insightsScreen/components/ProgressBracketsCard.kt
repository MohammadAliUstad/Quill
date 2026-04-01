package com.yugentech.quill.ui.more.insightsScreen.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.insghts.ProgressBrackets
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

@Composable
fun ProgressBracketsCard(
    progressBrackets: ProgressBrackets,
    modifier: Modifier = Modifier
) {
    val total =
        (progressBrackets.notStarted + progressBrackets.inProgress + progressBrackets.finished)
            .coerceAtLeast(1).toFloat()

    val finishedFraction = progressBrackets.finished / total
    val inProgressFraction = progressBrackets.inProgress / total
    val notStartedFraction = progressBrackets.notStarted / total

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {
        // Segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(MaterialTheme.corners.extraLarge)),
        ) {
            if (finishedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(finishedFraction)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            if (inProgressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(inProgressFraction)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
            if (notStartedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(notStartedFraction)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BracketLegendItem(
                label = "Finished",
                count = progressBrackets.finished,
                color = MaterialTheme.colorScheme.primary
            )
            BracketLegendItem(
                label = "In Progress",
                count = progressBrackets.inProgress,
                color = MaterialTheme.colorScheme.secondary,
                align = TextAlign.Center
            )
            BracketLegendItem(
                label = "Not Started",
                count = progressBrackets.notStarted,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                align = TextAlign.End
            )
        }
    }
}

@Composable
private fun BracketLegendItem(
    label: String,
    count: Int,
    color: Color,
    align: TextAlign = TextAlign.Start
) {
    Column(
        horizontalAlignment = when (align) {
            TextAlign.Center -> Alignment.CenterHorizontally
            TextAlign.End -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = align
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align
        )
    }
}