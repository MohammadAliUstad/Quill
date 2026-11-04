package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import kotlin.math.ceil

/**
 * Text that, once it wraps, is only as wide as its longest line. A plain Text takes all the width
 * it's given as soon as it wraps, which leaves dead space after the lines in a chat bubble.
 */
@Composable
fun TightText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Filled by onTextLayout, which runs inside the Text's own measure, so it's ready right after.
    val lastLayout = remember { arrayOfNulls<TextLayoutResult>(1) }
    Text(
        text = text,
        style = style,
        color = color,
        onTextLayout = { lastLayout[0] = it },
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val result = lastLayout[0]
            val width = if (result == null || result.lineCount < 2) {
                placeable.width
            } else {
                val widest = (0 until result.lineCount).maxOf { result.getLineRight(it) - result.getLineLeft(it) }
                ceil(widest).toInt().coerceIn(constraints.minWidth, placeable.width)
            }
            layout(width, placeable.height) { placeable.place(0, 0) }
        }
    )
}
