package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import com.yugentech.quill.aira.util.stripMarkdown

/**
 * Long-press copies [text] to the clipboard with a haptic tick, as in Amatyaa's chat. It copies the
 * text as shown, without any markdown markers. Android shows its own confirmation that the text
 * was copied.
 *
 * With a [shape] (a bubble), the press shows a ripple clipped to that shape. Without one (plain
 * reply text) there's no ripple and no clip: the haptic tick is the feedback, and a clip would
 * cut into the letters at the text's corners.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.longPressToCopy(
    text: String,
    shape: Shape? = null
): Modifier = composed {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val onLongClick = {
        clipboardManager.setText(AnnotatedString(text.stripMarkdown()))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    if (shape != null) {
        this
            .clip(shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    } else {
        this.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = onLongClick
        )
    }
}
