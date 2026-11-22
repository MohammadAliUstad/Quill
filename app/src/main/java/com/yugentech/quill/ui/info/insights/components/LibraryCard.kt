package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.LibraryStats
import com.yugentech.theme.tokens.spacing

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
