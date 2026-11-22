package com.yugentech.quill.ui.access.onboarding.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.access.common.components.AiraScene
import kotlin.math.abs
import kotlinx.coroutines.launch

/** The onboarding's scenes, in order. */
private enum class Scene { FirstLine, Library, Aira, Habit }

@Composable
fun OnboardingContent(onFinish: () -> Unit, finishLabel: String) {
    val scenes = Scene.entries
    val pager = rememberPagerState { scenes.size }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val onLast = pager.currentPage == scenes.lastIndex
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
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onFinish()
                    },
                    enabled = !onLast,
                    modifier = Modifier.graphicsLayer { alpha = skipAlpha }
                ) {
                    Text(stringResource(R.string.skip), style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f)
            ) { page ->
                // A little depth while swiping: the scene drifts more slowly than the page and
                // eases back as it goes. It's fully faded out before the swipe ends, so a scene
                // the pager drops is never still on screen when it goes.
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
                        Scene.Aira -> AiraScene(active)
                        Scene.Habit -> HabitScene(active)
                    }
                }
            }

            // Back (from the second scene) + the main button
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = pager.currentPage > 0,
                    // clip = false: the slot widens without slicing the round button; it
                    // grows into place instead of being uncovered.
                    enter = expandHorizontally(tween(300, easing = FastOutSlowInEasing), clip = false) +
                        scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.7f) +
                        fadeIn(tween(300)),
                    exit = shrinkHorizontally(tween(300, easing = FastOutSlowInEasing), clip = false) +
                        scaleOut(tween(200), targetScale = 0.7f) +
                        fadeOut(tween(150))
                ) {
                    Row {
                        BackAction(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                        })
                        Spacer(Modifier.width(12.dp))
                    }
                }
                Button(
                    onClick = {
                        if (onLast) {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onFinish()
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                ) {
                    // Only the label swaps; the arrow stays put beside it and glides to its new
                    // place as the label's width eases to the new text's
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = onLast,
                            // A plain vertical swap: the new label pushes the old one out, upward on
                            // the way to the last scene and downward on the way back
                            transitionSpec = {
                                val direction = if (targetState) 1 else -1
                                (slideInVertically(tween(300)) { it * direction } + fadeIn(tween(300)))
                                    .togetherWith(slideOutVertically(tween(300)) { -it * direction } + fadeOut(tween(300)))
                                    // clip = false, as for the back button: the label area widens without
                                    // slicing the longer text; the button's own shape still bounds it
                                    .using(SizeTransform(clip = false) { _, _ -> tween(300) })
                            },
                            contentAlignment = Alignment.Center,
                            label = "onboardingButton"
                        ) { last ->
                            Text(
                                text = if (last) finishLabel else stringResource(R.string.onboarding_continue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
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
