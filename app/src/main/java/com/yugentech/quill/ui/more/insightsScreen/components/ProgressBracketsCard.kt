package com.yugentech.quill.ui.more.insightsScreen.components

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

    var targetFinished by remember { mutableFloatStateOf(0f) }
    var targetInProgress by remember { mutableFloatStateOf(0f) }
    var targetNotStarted by remember { mutableFloatStateOf(0f) }

    val animFinished by animateFloatAsState(targetFinished, tween(600), label = "finished")
    val animInProgress by animateFloatAsState(
        targetInProgress,
        tween(600, delayMillis = 100),
        label = "inProgress"
    )
    val animNotStarted by animateFloatAsState(
        targetNotStarted,
        tween(600, delayMillis = 200),
        label = "notStarted"
    )

    LaunchedEffect(progressBrackets) {
        targetFinished = finishedFraction
        targetInProgress = inProgressFraction
        targetNotStarted = notStartedFraction
    }

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
            if (animFinished > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animFinished)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            if (animInProgress > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animInProgress)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
            if (animNotStarted > 0f) {
                Box(
                    modifier = Modifier
                        .weight(animNotStarted)
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