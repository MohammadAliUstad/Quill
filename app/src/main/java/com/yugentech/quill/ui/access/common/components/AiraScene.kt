package com.yugentech.quill.ui.access.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.TightText
import kotlinx.coroutines.delay

/** The reply appears a few words at a time: this many words per step, this far apart, each fading in over [CHUNK_FADE_MS]. */
private const val WORDS_PER_CHUNK = 3
private const val CHUNK_STEP_MS = 110
private const val CHUNK_FADE_MS = 250

/**
 * Aira, still in her circle, answering a question the way Hajime's Sensei does in its onboarding:
 * the reader's bubble arrives, the thinking dots pulse, then the reply grows across the full
 * width a few words at a time, each group fading in. It plays once; coming back to the scene
 * shows the finished exchange. Shown in onboarding and on the guest Aira screen.
 */
@Composable
internal fun AiraScene(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val portraitIn = rememberEntrance(active, 100)
    val lockIn = rememberEntrance(active, 350)
    val ruleIn = rememberEntrance(active, 250)
    var played by rememberSaveable { mutableStateOf(false) }
    // Whether the exchange had already played when this scene was composed: then it's shown finished
    val alreadyPlayed = remember { played }
    var asked by remember { mutableStateOf(played) }
    var thinking by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(played) }
    LaunchedEffect(active) {
        if (!active || played) return@LaunchedEffect
        delay(600)
        asked = true
        delay(400)
        thinking = true
        delay(900)
        thinking = false
        answered = true
        played = true
    }
    val questionAlpha by animateFloatAsState(if (asked) 1f else 0f, tween(250), label = "airaQuestion")
    val dotsAlpha by animateFloatAsState(if (thinking) 1f else 0f, tween(if (thinking) 250 else 150), label = "airaThinking")

    SceneColumn {
        // Aira's circle from the chat's empty state, at 4/5 of its size. She stays still: she only
        // fades in, so the eye goes to the conversation.
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer { alpha = portraitIn.value.coerceIn(0f, 1f) }
                .border(width = 1.dp, color = colors.primary, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.surface)
                .background(colors.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Image(
                painter = painterResource(R.drawable.psmile_calm),
                contentDescription = "Aira",
                modifier = Modifier
                    .requiredSize(144.dp)
                    .offset(x = (-1.6).dp, y = 12.8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        // How far into the book she's allowed to talk about
        Hint(stringResource(R.string.onboarding_aira_lock), Icons.Default.Lock, Modifier.rise({ lockIn.value }, 10.dp))
        Spacer(Modifier.height(16.dp))

        // The chat: 12dp between messages. Everything is laid out from the start (just unseen),
        // so the scene never shifts as the exchange plays and the reply ends right above the rule.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.graphicsLayer { alpha = questionAlpha }) {
                DemoQuestionBubble(stringResource(R.string.onboarding_aira_question))
            }
            Box {
                DemoReply(
                    text = stringResource(R.string.onboarding_aira_reply),
                    started = answered,
                    animate = !alreadyPlayed
                )
                // The thinking dots sit where the reply will appear
                if (dotsAlpha > 0f) {
                    Box(Modifier.graphicsLayer { alpha = dotsAlpha }) { DemoThinkingDots() }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        // A quiet rule setting the conversation apart from the words about it
        SceneRule(Modifier.graphicsLayer { alpha = ruleIn.value.coerceIn(0f, 1f) })
        Spacer(Modifier.height(20.dp))
        SceneText(
            title = stringResource(R.string.onboarding_aira_title),
            body = stringResource(R.string.onboarding_aira_body),
            active = active,
            delayMs = 300
        )
    }
}

/** The reader's question: a primary bubble on the right, squared at the top right, at most 78% wide. */
@Composable
private fun DemoQuestionBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.78f
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                // The primary: the chat's primary-container bubble would vanish into the aura
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.widthIn(max = bubbleMaxWidth)
            ) {
                TightText(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/** Three primary dots pulsing between faint and full, each 200ms behind the one before. */
@Composable
private fun DemoThinkingDots() {
    val transition = rememberInfiniteTransition(label = "thinking")
    Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 200)
                    ),
                    label = "dot$index"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                )
            }
        }
    }
}

/**
 * Aira's reply across the full width, no bubble: once [started], it appears a few words at a
 * time, each group fading in, so it builds line by line as if arriving. Without [animate] it's
 * shown whole. The full text is always laid out, so its height never changes.
 */
@Composable
private fun DemoReply(text: String, started: Boolean, animate: Boolean) {
    val color = MaterialTheme.colorScheme.onBackground
    val chunks = remember(text) { text.split(" ").chunked(WORDS_PER_CHUNK).map { it.joinToString(" ") } }
    val total = (chunks.size - 1) * CHUNK_STEP_MS + CHUNK_FADE_MS
    val elapsed = remember(text) { Animatable(if (animate) 0f else total.toFloat()) }
    LaunchedEffect(started) {
        if (started) elapsed.animateTo(total.toFloat(), tween(total, easing = LinearEasing))
    }

    val now = elapsed.value
    Text(
        text = buildAnnotatedString {
            chunks.forEachIndexed { i, chunk ->
                val since = now - i * CHUNK_STEP_MS
                // Groups not started yet keep their place, unseen
                val alpha = (since / CHUNK_FADE_MS).coerceIn(0f, 1f)
                withStyle(SpanStyle(color = color.copy(alpha = alpha))) {
                    append(if (i == 0) chunk else " $chunk")
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

/**
 * A hairline across the scene that fades out at both ends, with Aira's star (the ✦ from her
 * chat's message field) at its centre: a quiet break between the conversation and the words about it.
 */
@Composable
private fun SceneRule(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, line)))
        )
        Text(
            text = "✦",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(line, Color.Transparent)))
        )
    }
}
