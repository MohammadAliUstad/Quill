package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.yugentech.quill.aira.util.stripMarkdown

private const val MS_PER_CHAR = 6f
private const val MIN_DURATION_MS = 10
private const val MAX_DURATION_MS = 3500

// Reveals text character by character, like it's being typed. Markdown is stripped first since
// the prompts are meant to return plain text -- this is a safety net for whatever slips through
// anyway, not something meant to render as markdown.
//
// [resetKey] controls when the reveal restarts from zero: pass something stable (e.g. a message
// id) that only changes for a genuinely new message, not on every recomposition of the same one
// -- otherwise re-displaying an already-typed message (e.g. after scrolling it back into view)
// would replay the animation.
@Composable
fun TypewriterText(
    text: String,
    resetKey: Any,
    startRevealed: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    onTypingStateChange: (Boolean) -> Unit = {}
) {
    val cleanText = remember(text) { text.stripMarkdown() }

    val revealedChars = remember(resetKey) {
        Animatable(if (startRevealed) cleanText.length.toFloat() else 0f)
    }

    LaunchedEffect(cleanText) {
        if (revealedChars.value < cleanText.length) {
            onTypingStateChange(true)

            // Per-character speed with a hard cap on the total, so short answers still visibly
            // type out while huge ones finish in a few seconds instead of scaling without limit.
            val charsRemaining = cleanText.length - revealedChars.value
            revealedChars.animateTo(
                targetValue = cleanText.length.toFloat(),
                animationSpec = tween(
                    durationMillis = (charsRemaining * MS_PER_CHAR).toInt()
                        .coerceIn(MIN_DURATION_MS, MAX_DURATION_MS),
                    easing = LinearEasing
                )
            )

            onTypingStateChange(false)
        }
    }

    Text(
        text = cleanText.substring(0, revealedChars.value.toInt().coerceAtMost(cleanText.length)),
        style = style,
        color = color,
        modifier = modifier
    )
}
