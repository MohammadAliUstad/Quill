package com.yugentech.quill.ui.access.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.access.onboarding.components.AiraScene
import com.yugentech.quill.ui.access.onboarding.components.FirstLineScene
import com.yugentech.quill.ui.access.onboarding.components.HabitScene
import com.yugentech.quill.ui.access.onboarding.components.LibraryScene
import com.yugentech.quill.ui.access.onboarding.components.ReaderScene
import com.yugentech.quill.ui.access.onboarding.components.AuraTheme
import com.yugentech.quill.ui.access.onboarding.components.LocalAuraBackground
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The onboarding's scenes, in order. */
private enum class Scene { FirstLine, Library, Reader, Aira, Habit }

/**
 * Quill's first run: five short, interactive scenes rather than a feature list. A famous first
 * line writes itself onto a page that turns at a tap, classics slide onto a shelf, a page of the
 * reader changes theme and size live, Aira answers a question without spoiling the book, and a
 * reading heatmap fills in.
 *
 * It sits on the aura the nav host draws behind it (shared with the sign-in screens), whose
 * glows glide with each step; everything on it is white (AuraTheme). Story-style bars at the top fill as they go; Skip leaves at any point, and the button
 * at the bottom moves on, then starts reading on the last scene.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    // White on the shared aura, which the nav host draws behind this screen
    AuraTheme { OnboardingContent(onFinish) }
}

@Composable
private fun OnboardingContent(onFinish: () -> Unit) {
    val scenes = Scene.entries
    val pager = rememberPagerState { scenes.size }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val onLast = pager.currentPage == scenes.lastIndex
    // The glows glide with each step, following the swipe
    val aura = LocalAuraBackground.current
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage + pager.currentPageOffsetFraction }.collect { aura.travel = it }
    }

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Story bars and Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryBars(pager = pager, count = scenes.size, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                val skipAlpha by animateFloatAsState(if (onLast) 0f else 1f, tween(200), label = "skip")
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onFinish()
                    },
                    enabled = !onLast,
                    modifier = Modifier.graphicsLayer { alpha = skipAlpha }
                ) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f)
            ) { page ->
                // A little depth while swiping: the scene drifts more slowly than the page and
                // eases back as it goes. It's fully faded out before the swipe ends: held back
                // like this it's still partly on screen at the end, and a scene the pager drops
                // while still visible vanishes in one frame.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                            val away = abs(offset).coerceIn(0f, 1f)
                            translationX = offset * size.width * 0.35f
                            alpha = (1f - away / FADE_OUT_BY).coerceIn(0f, 1f)
                            scaleX = 1f - away * 0.08f
                            scaleY = 1f - away * 0.08f
                        }
                ) {
                    val active = pager.currentPage == page
                    when (scenes[page]) {
                        Scene.FirstLine -> FirstLineScene(active)
                        Scene.Library -> LibraryScene(active)
                        Scene.Reader -> ReaderScene(active)
                        Scene.Aira -> AiraScene(active)
                        Scene.Habit -> HabitScene(active)
                    }
                }
            }

            Button(
                onClick = {
                    if (onLast) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onFinish()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(58.dp)
            ) {
                AnimatedContent(
                    targetState = onLast,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 })
                            .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 2 })
                    },
                    label = "onboardingButton"
                ) { last ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (last) "Start reading" else "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** How far through a swipe (0..1) a leaving scene is fully faded out. */
private const val FADE_OUT_BY = 0.6f

/**
 * One bar per scene, like stories: finished scenes full, the current one filling as the reader
 * swipes toward the next, later ones empty.
 */
@Composable
private fun StoryBars(pager: PagerState, count: Int, modifier: Modifier = Modifier) {
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
