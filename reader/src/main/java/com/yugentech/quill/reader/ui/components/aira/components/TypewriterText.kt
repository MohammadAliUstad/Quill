package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.yugentech.quill.aira.util.stripMarkdown
import kotlin.math.roundToInt

/** A chunk closes once it holds this many characters: a few words, like a streamed token run. */
private const val CHUNK_CHARS = 14

/** How long each chunk takes to fade in. */
private const val CHUNK_FADE_MS = 320f

/** The gap between one chunk starting to appear and the next; shrinks for long replies. */
private const val MAX_CHUNK_INTERVAL_MS = 60f

/** However long the reply, the last chunk starts appearing within this time. */
private const val MAX_REVEAL_SPREAD_MS = 2_200f

private val SENTENCE_END = setOf('.', '!', '?', ':', ';')

/**
 * Where each chunk of [text] starts (character offsets, ascending, first is 0). Chunks are runs
 * of whole words, closed at [CHUNK_CHARS], at the end of a sentence, or at a line break.
 */
private fun chunkStarts(text: String): IntArray {
    val starts = mutableListOf(0)
    var chunkStart = 0
    var i = 0
    while (i < text.length) {
        // Advance past one word and the whitespace after it.
        while (i < text.length && !text[i].isWhitespace()) i++
        val wordEnd = i
        var lineBreak = false
        while (i < text.length && text[i].isWhitespace()) {
            if (text[i] == '\n') lineBreak = true
            i++
        }
        if (i >= text.length) break
        val chars = wordEnd - chunkStart
        val sentenceEnds = wordEnd > 0 && text[wordEnd - 1] in SENTENCE_END
        if (lineBreak || chars >= CHUNK_CHARS || (sentenceEnds && chars >= 4)) {
            starts += i
            chunkStart = i
        }
    }
    return starts.toIntArray()
}

private fun intervalFor(chunks: Int): Float =
    if (chunks <= 1) 0f else minOf(MAX_CHUNK_INTERVAL_MS, MAX_REVEAL_SPREAD_MS / (chunks - 1))

private fun totalMillisFor(chunks: Int): Float = (chunks - 1).coerceAtLeast(0) * intervalFor(chunks) + CHUNK_FADE_MS

/**
 * How tall the text is [t] ms into the reveal. Each line's bottom is a keyframe at the moment its
 * first chunk starts fading in, and the height moves linearly from one keyframe to the next, so
 * it grows at a steady pace across lines (opening each line just as its words arrive) instead of
 * jumping a line at a time.
 */
private fun revealedHeight(result: TextLayoutResult, starts: IntArray, interval: Float, t: Float): Float {
    var prevTime = 0f
    var prevBottom = result.getLineBottom(result.getLineForOffset(starts[0]))
    for (c in 1 until starts.size) {
        val bottom = result.getLineBottom(result.getLineForOffset(starts[c]))
        if (bottom <= prevBottom) continue
        val time = c * interval
        if (t < time) {
            val fraction = ((t - prevTime) / (time - prevTime)).coerceIn(0f, 1f)
            return prevBottom + (bottom - prevBottom) * fraction
        }
        prevTime = time
        prevBottom = bottom
    }
    return prevBottom
}

// Reveals text the way ChatGPT and Claude show a streamed reply: in chunks of a few words, each
// fading in a moment after the one before, so several are mid-fade at once. Markdown is stripped
// first since the prompts are meant to return plain text -- this is a safety net for whatever
// slips through anyway, not something meant to render as markdown.
//
// The whole text is laid out from the start and the parts not yet shown are masked out while
// drawing, so the text never reflows mid-reveal and the animation doesn't recompose it.
//
// With [growWithReveal], the measured height follows the reveal instead of taking the full text
// height up front, so a container anchored at the bottom (like a reverse-layout chat list) is
// pushed up line by line as the reply appears rather than all at once.
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
    growWithReveal: Boolean = false,
    onTypingStateChange: (Boolean) -> Unit = {}
) {
    val cleanText = remember(text) { text.stripMarkdown() }
    val starts = remember(cleanText) { chunkStarts(cleanText) }
    val interval = intervalFor(starts.size)
    val total = totalMillisFor(starts.size)

    // Milliseconds into the reveal.
    val elapsed = remember(resetKey) { Animatable(if (startRevealed) Float.MAX_VALUE else 0f) }

    LaunchedEffect(cleanText) {
        if (elapsed.value < total) {
            onTypingStateChange(true)
            elapsed.animateTo(
                targetValue = total,
                animationSpec = tween(
                    durationMillis = (total - elapsed.value).toInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
            onTypingStateChange(false)
        }
    }

    // Held outside composition state reads: only the draw phase looks at it.
    val textLayout = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = cleanText,
        style = style,
        color = color,
        onTextLayout = { textLayout.value = it },
        modifier = modifier
            .then(
                if (growWithReveal) {
                    Modifier
                        // Clip at the grown height so a line still gliding in can't spill below.
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val t = elapsed.value
                            val result = textLayout.value
                            val height = if (t >= total || result == null) placeable.height
                            else revealedHeight(result, starts, interval, t).roundToInt()
                                .coerceIn(0, placeable.height)
                            layout(placeable.width, height) { placeable.place(0, 0) }
                        }
                } else Modifier
            )
            // Offscreen so the erasing below only removes this text, not what's behind it.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val result = textLayout.value ?: return@drawWithContent
                val t = elapsed.value
                if (t >= total) return@drawWithContent

                for (c in starts.indices) {
                    val alpha = ((t - c * interval) / CHUNK_FADE_MS).coerceIn(0f, 1f)
                    if (alpha >= 1f) continue
                    val start = starts[c]
                    if (alpha <= 0f) {
                        // This chunk and every later one haven't started: erase the rest of
                        // this line from the chunk on, then every line below.
                        val line = result.getLineForOffset(start)
                        val x = result.getHorizontalPosition(start, usePrimaryDirection = true)
                        val top = result.getLineTop(line)
                        val bottom = result.getLineBottom(line)
                        drawRect(Color.Black, Offset(x, top), Size(size.width - x, bottom - top), blendMode = BlendMode.DstOut)
                        drawRect(Color.Black, Offset(0f, bottom), Size(size.width, size.height - bottom), blendMode = BlendMode.DstOut)
                        break
                    }
                    // Mid-fade: dim exactly this chunk's glyphs.
                    val end = if (c + 1 < starts.size) starts[c + 1] else cleanText.length
                    drawPath(
                        path = result.getPathForRange(start, end),
                        color = Color.Black.copy(alpha = 1f - alpha),
                        blendMode = BlendMode.DstOut
                    )
                }
            }
    )
}
