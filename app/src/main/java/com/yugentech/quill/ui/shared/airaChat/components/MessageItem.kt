package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.ui.components.aira.components.ThinkingIndicator

@Composable
fun MessageItem(
    message: ChatMessage,
    onTypingStateChange: (Boolean) -> Unit = {}
) {
    val isAira = message.isFromAira
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = (containerWidth * 0.78f).dp

    if (isAira) {
        val isThinking = message.isNew && message.text.isBlank()

        val textLength = remember(message.stableKey) {
            androidx.compose.animation.core.Animatable(if (message.isNew) 0f else message.text.length.toFloat())
        }

        LaunchedEffect(message.text) {
            if (textLength.value < message.text.length) {
                onTypingStateChange(true)

                val charsRemaining = message.text.length - textLength.value
                textLength.animateTo(
                    targetValue = message.text.length.toFloat(),
                    animationSpec = tween(
                        durationMillis = (charsRemaining * 15f).toInt().coerceAtLeast(10),
                        easing = LinearEasing
                    )
                )

                onTypingStateChange(false)
            }
        }

        val displayedText =
            message.text.substring(0, textLength.value.toInt().coerceAtMost(message.text.length))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Aira",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            if (isThinking) {
                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    ThinkingIndicator()
                }
            } else {
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .padding(top = 4.dp)
                )
            }
        }
    } else {
        var visible by remember(message.stableKey) {
            mutableStateOf(!message.isNew)
        }
        LaunchedEffect(message.stableKey) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 4.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 18.dp
                    ),
                    modifier = Modifier.widthIn(max = maxWidth)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}