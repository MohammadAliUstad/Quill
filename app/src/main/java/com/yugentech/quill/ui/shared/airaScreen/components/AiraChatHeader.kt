package com.yugentech.quill.ui.shared.airaScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R

@Composable
fun AiraChatHeader(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    isStreaming: Boolean,
    isVisuallyTyping: Boolean,
    lastChapterTitle: String?,
    spoilerLockEnabled: Boolean
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to surfaceColor,
                    0.4f to surfaceColor,
                    0.55f to surfaceColor.copy(alpha = 0.85f),
                    0.75f to surfaceColor.copy(alpha = 0.4f),
                    0.9f to surfaceColor.copy(alpha = 0.1f),
                    1.0f to Color.Transparent
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(
                        id = when {
                            isStreaming || isVisuallyTyping -> R.drawable.pexplaining
                            isLoading -> R.drawable.preading
                            else -> R.drawable.psmile_calm
                        }
                    ),
                    contentDescription = "Aira",
                    modifier = Modifier
                        .requiredSize(116.dp)
                        .offset(x = (-2).dp, y = 12.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Current Chapter",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastChapterTitle ?: "Start of book",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                val backgroundColor by animateColorAsState(
                    targetValue = if (spoilerLockEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    label = "spoilerBgColor"
                )

                val textColor by animateColorAsState(
                    targetValue = if (spoilerLockEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "spoilerTextColor"
                )

                Surface(
                    shape = CircleShape,
                    color = backgroundColor,
                    modifier = Modifier.offset(x = (-10).dp)
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
                            text = if (isLocked) "Spoiler lock active" else "Spoiler lock disabled",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}