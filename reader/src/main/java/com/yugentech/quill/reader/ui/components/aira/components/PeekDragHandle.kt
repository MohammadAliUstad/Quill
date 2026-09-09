package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Just the visual pill now -- the draggable touch target itself is the header it's overlaid on
// (see AiraPeekBar.kt), rather than a separate fixed-height row, so the drag gesture no longer
// costs its own dedicated strip of vertical space.
@Composable
fun PeekDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}
