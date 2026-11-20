package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.yugentech.quill.reader.ui.components.aira.components.ThinkingIndicator
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.access.subscriptions.parent.planOptions
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@Composable
fun SubscribeBottomBar(
    selectedPlanIndex: Int,
    isRestoring: Boolean,
    onSubscribeClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Action: Subscribe
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSubscribeClick()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(R.string.get_plan_pro, stringResource(planOptions[selectedPlanIndex].label)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Secondary Action: Restore
                val restoringDescription = stringResource(R.string.restoring)
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onRestoreClick()
                    },
                    enabled = !isRestoring,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(
                        targetState = isRestoring,
                        // The label slides up and out as Aira's thinking dots rise in from below,
                        // both at once (and back the same way when it's done). Clipped to the button,
                        // so neither shows outside it while moving.
                        transitionSpec = {
                            val enter = tween<IntOffset>(
                                durationMillis = AppAnimations.Durations.Smooth,
                                easing = AppAnimations.Easings.EmphasizedDecelerate
                            )
                            val exit = tween<IntOffset>(
                                durationMillis = AppAnimations.Durations.Smooth,
                                easing = AppAnimations.Easings.Accelerate
                            )
                            (slideInVertically(enter) { it } + fadeIn(
                                tween(AppAnimations.Durations.Smooth)
                            )) togetherWith (slideOutVertically(exit) { -it } + fadeOut(
                                tween(AppAnimations.Durations.Rapid)
                            )) using SizeTransform(clip = true)
                        },
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clipToBounds(),
                        label = "restore_button_state"
                    ) { restoring ->
                        // The label is laid out either way, so the button keeps its width while
                        // the dots show, and the Pro button beside it doesn't shift
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.restore),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.alpha(if (restoring) 0f else 1f)
                            )
                            if (restoring) {
                                ThinkingIndicator(
                                    modifier = Modifier.semantics {
                                        contentDescription = restoringDescription
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.cancel_anytime),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}