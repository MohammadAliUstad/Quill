package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusBanner(
    isIndexing: Boolean,
    indexingProgress: Int = 0,
    error: String?,
    onDismiss: () -> Unit = {}
) {
    val (bgColor, textColor, message) = when {
        isIndexing -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            if (indexingProgress > 0) "Aira is reading this book... $indexingProgress%"
            else "Aira is reading this book..."
        )

        error != null -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            error
        )

        else -> return
    }

    val animatedProgress by animateFloatAsState(
        targetValue = indexingProgress / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "IndexingProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(enabled = error != null, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isIndexing) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.7f))
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (error != null) {
                Text(
                    "✕",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }

        if (isIndexing) {
            if (indexingProgress > 0) {
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small),
                    color = textColor,
                    trackColor = textColor.copy(alpha = 0.2f)
                )
            } else {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small),
                    color = textColor,
                    trackColor = textColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}