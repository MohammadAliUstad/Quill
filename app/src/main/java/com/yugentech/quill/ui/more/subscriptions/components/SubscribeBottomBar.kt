package com.yugentech.quill.ui.more.subscriptions.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.more.subscriptions.parent.planOptions

@Composable
fun SubscribeBottomBar(
    selectedPlanIndex: Int,
    isRestoring: Boolean,
    onSubscribeClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
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
                    onClick = onSubscribeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Get ${planOptions[selectedPlanIndex].label} Pro",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Secondary Action: Restore (Now Animated)
                TextButton(
                    onClick = onRestoreClick,
                    enabled = !isRestoring,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(
                        targetState = isRestoring,
                        transitionSpec = {
                            // Fade + Scale In for the new state, simple fade out for the old one
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                        label = "restore_button_transition"
                    ) { stateIsRestoring ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (stateIsRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Restoring",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Text(
                                    text = "Restore",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cancel anytime. Terms and conditions apply.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}