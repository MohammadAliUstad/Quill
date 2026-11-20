package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.theme.WindSongFont
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiraEmptyState(
    lastChapterTitle: String?,
    isIndexing: Boolean,
    // Waiting its turn behind other books in the indexing queue
    isQueued: Boolean,
    // Aira has finished reading the book
    isReady: Boolean,
    indexingProgress: Int,
    spoilerLockEnabled: Boolean,
    hasStartedReading: Boolean,
    modifier: Modifier = Modifier,
    onToggleSpoilerLock: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // While Aira reads the book: how far she's got, a few dp above her circle. The line keeps
        // its space when hidden, so the circle doesn't jump when indexing finishes.
        val animatedProgress by animateFloatAsState(
            targetValue = indexingProgress / 100f,
            animationSpec = tween(durationMillis = 600),
            label = "IndexingProgress"
        )
        val percentAlpha by animateFloatAsState(
            targetValue = if (isIndexing && indexingProgress > 0) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "IndexingPercentAlpha"
        )
        Text(
            text = "${(animatedProgress * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { alpha = percentAlpha }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // The wave runs from one edge of the screen to the other (past this column's 16dp padding),
        // through the middle of the circle and behind it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val bleed = (16.dp * 2).roundToPx()
                    val fullWidth = constraints.maxWidth + bleed
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = fullWidth, maxWidth = fullWidth)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(-bleed / 2, 0)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Fades out when indexing ends, then leaves the composition.
            val waveAlpha by animateFloatAsState(
                targetValue = if (isIndexing) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "IndexingWaveAlpha"
            )
            if (isIndexing || waveAlpha > 0f) {
                val waveModifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = waveAlpha }
                if (indexingProgress > 0) {
                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = waveModifier,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                } else {
                    LinearWavyProgressIndicator(
                        modifier = waveModifier,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    // An opaque base under the tint, so the wave passes behind the circle
                    // instead of showing through it.
                    .background(MaterialTheme.colorScheme.surface)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(
                        id = if (isIndexing) R.drawable.preading else R.drawable.psmile_calm
                    ),
                    contentDescription = "Aira",
                    modifier = Modifier
                        .requiredSize(180.dp)
                        .offset(x = (-2).dp, y = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aira",
            fontFamily = WindSongFont,
            fontSize = 56.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.aira_tagline),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isQueued -> stringResource(R.string.aira_empty_queued)
                isIndexing || !isReady -> stringResource(R.string.aira_empty_indexing)
                !hasStartedReading -> stringResource(R.string.aira_empty_not_started)
                else -> stringResource(R.string.aira_empty_ready)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // The chapter and spoiler lock come with the chat itself, once Aira has read the book
        AnimatedVisibility(
            visible = isReady && hasStartedReading,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 4 }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.current_chapter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = lastChapterTitle ?: stringResource(R.string.just_started_reading),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val backgroundColor by animateColorAsState(
                        targetValue = if (spoilerLockEnabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        label = "spoilerBgColor"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (spoilerLockEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "spoilerTextColor"
                    )

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(
                                if (spoilerLockEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                            )
                            onToggleSpoilerLock()
                        },
                        shape = CircleShape,
                        color = backgroundColor
                    ) {
                        AnimatedContent(
                            targetState = spoilerLockEnabled,
                            transitionSpec = {
                                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 }) togetherWith
                                        (fadeOut(tween(100)) + slideOutVertically(tween(200)) { -it / 2 })
                            },
                            label = "spoilerTextAnimation"
                        ) { isLocked ->
                            Text(
                                text = if (isLocked) stringResource(R.string.spoiler_lock_active) else stringResource(R.string.spoiler_lock_disabled),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}