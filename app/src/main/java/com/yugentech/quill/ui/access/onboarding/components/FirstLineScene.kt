package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.ui.access.common.components.Hint
import com.yugentech.quill.ui.access.common.components.SceneColumn
import com.yugentech.quill.ui.access.common.components.SceneText
import com.yugentech.quill.ui.access.common.components.rememberEntrance
import com.yugentech.quill.ui.access.common.components.rise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Opening(val line: String, val source: String)

/** Famous first lines, all from public-domain classics Quill's free library carries. */
private val Openings = listOf(
    Opening("It was the best of times, it was the worst of times.", "Charles Dickens, A Tale of Two Cities"),
    Opening("Call me Ishmael.", "Herman Melville, Moby-Dick"),
    Opening("It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.", "Jane Austen, Pride and Prejudice"),
    Opening("All happy families are alike; each unhappy family is unhappy in its own way.", "Leo Tolstoy, Anna Karenina")
)

/** How the page beneath sits while the top one is at rest: a little smaller, lower and fainter. */
private const val UNDER_SCALE = 0.94f
private val UNDER_DROP = 12.dp
private const val UNDER_ALPHA = 0.6f

/** How long the page beneath takes to ease up into place, the first time and after each tap. */
private const val UNDER_SETTLE_MS = 420

/** On the scene's first look, how long after it opens the page beneath starts easing up. */
private const val UNDER_FIRST_DELAY_MS = 550L

/**
 * A page with a famous first line on it, written in word by word, on a small stack. Tap it and
 * the page lifts off the stack, the one beneath rises into its place, and the next great opening
 * is written onto it.
 */
@Composable
internal fun FirstLineScene(active: Boolean) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pageIn = rememberEntrance(active, 150)
    val hintIn = rememberEntrance(active, 2200)
    var index by remember { mutableIntStateOf(0) }
    // 0 = the page at rest on the stack, 1 = lifted away, the page beneath risen into its place
    val lift = remember { Animatable(0f) }
    // The next page beneath, easing into place once the last one has risen: 0 = not there yet
    // (a little lower still, and transparent), 1 = resting under the top page. It starts out of
    // sight too, so on the scene's first look it eases up under the top page exactly as it does
    // after every tap, rather than arriving already in place.
    val underIn = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active && underIn.value < 1f && !lift.isRunning) {
            // Once the top page has mostly risen in
            delay(UNDER_FIRST_DELAY_MS)
            underIn.animateTo(1f, tween(UNDER_SETTLE_MS, easing = FastOutSlowInEasing))
        }
    }

    SceneColumn {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer {
                    val t = pageIn.value
                    alpha = t.coerceIn(0f, 1f)
                    translationY = (1f - t) * 40.dp.toPx()
                }
        ) {
            // The blank page beneath: tucked a little lower and smaller, rising to take the top
            // page's place as it lifts away
            PaperPage(
                modifier = Modifier.graphicsLayer {
                    val p = lift.value
                    val settle = 1f - underIn.value
                    val scale = UNDER_SCALE + (1f - UNDER_SCALE) * p - 0.03f * settle
                    scaleX = scale
                    scaleY = scale
                    translationY = ((1f - p) + settle) * UNDER_DROP.toPx()
                    alpha = (UNDER_ALPHA + (1f - UNDER_ALPHA) * p) * underIn.value
                }
            ) {}

            // The page with the line on it. Tapped, it lifts off to the left with a slight tilt
            // and fades; once it's gone the next opening is written onto the risen page.
            PaperPage(
                modifier = Modifier
                    .graphicsLayer {
                        val p = lift.value
                        translationX = -p * size.width * 0.45f
                        translationY = -p * 28.dp.toPx()
                        rotationZ = -p * 7f
                        val scale = 1f + 0.03f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - p
                    }
                    // No ripple: the page that lifts away snaps back as the next one, and a
                    // ripple still fading on it would flash across the fresh page.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Not until the page beneath is in place, so every lift starts from the same stack
                        if (lift.isRunning || underIn.value < 1f) return@clickable
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        scope.launch {
                            lift.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                            // The risen page now looks exactly like the top one at rest, blank:
                            // swap them back in the same frame and start writing the next line.
                            index = (index + 1) % Openings.size
                            // The next page beneath starts out of sight, then eases up into place
                            underIn.snapTo(0f)
                            lift.snapTo(0f)
                            underIn.animateTo(1f, tween(UNDER_SETTLE_MS, easing = FastOutSlowInEasing))
                        }
                    }
            ) {
                WrittenLine(opening = Openings[index], active = active, key = index)
            }
        }
        Spacer(Modifier.height(20.dp))
        Hint(stringResource(R.string.onboarding_firstline_tap), Icons.Default.TouchApp, Modifier.rise({ hintIn.value }, 12.dp))
        Spacer(Modifier.height(44.dp))
        SceneText(
            title = stringResource(R.string.onboarding_firstline_title),
            body = stringResource(R.string.onboarding_firstline_body),
            active = active,
            delayMs = 1400
        )
    }
}

/** A sheet of the book: paper-coloured, a faint spine shadow down its left edge, [content] inset. */
@Composable
private fun PaperPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
                                endX = 18.dp.toPx()
                            )
                        )
                    }
                }
                .padding(start = 28.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)
        ) {
            content()
        }
    }
}

/** [opening]'s line appearing a word at a time, then who wrote it. Starts over for each new [key]. */
@Composable
private fun WrittenLine(opening: Opening, active: Boolean, key: Int) {
    val colors = MaterialTheme.colorScheme
    val words = remember(opening) { opening.line.split(" ") }
    var shown by remember(key) { mutableIntStateOf(0) }
    // Who wrote it: hidden from a new page's very first frame (not faded out from the last
    // page's), and faded in only once its line is written.
    val sourceIn = remember(key) { Animatable(0f) }
    LaunchedEffect(key, active) {
        if (!active) return@LaunchedEffect
        // The first page waits for the scene to settle; new pages write straight away.
        delay(if (key == 0) 700 else 120)
        for (i in 1..words.size) {
            shown = i
            delay(85)
        }
        sourceIn.animateTo(1f, tween(400))
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = buildAnnotatedString {
                words.forEachIndexed { i, word ->
                    // Words not written yet keep their place, so the line never reflows
                    withStyle(SpanStyle(color = if (i < shown) colors.onSurface else Color.Transparent)) { append(word) }
                    if (i < words.lastIndex) append(" ")
                }
            },
            fontFamily = FontFamily.Serif,
            fontSize = if (opening.line.length > 80) 19.sp else 24.sp,
            lineHeight = if (opening.line.length > 80) 28.sp else 34.sp
        )
        Text(
            text = "— ${opening.source}",
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic,
            color = colors.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = sourceIn.value }
        )
    }
}
