package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * One bar per scene, like stories: finished scenes full, the current one filling as the reader
 * swipes toward the next, later ones empty.
 */
@Composable
fun StoryBars(pager: PagerState, count: Int, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    val fill = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(track)
                    .drawBehind {
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        val filled = (position + 1f - i).coerceIn(0f, 1f)
                        if (filled > 0f) {
                            drawRoundRect(
                                color = fill,
                                size = Size(size.width * filled, size.height),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                        }
                    }
            )
        }
    }
}
