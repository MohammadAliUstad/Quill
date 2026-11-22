package com.yugentech.quill.ui.book.airaChat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.ui.common.components.ThinkingIndicator
import com.yugentech.quill.reader.ui.common.components.longPressToCopy
import com.yugentech.quill.reader.ui.common.components.TypewriterText
import com.yugentech.quill.ui.common.components.TightText

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

        // If this reply leaves composition mid-reveal, its reveal never reports finishing, which
        // would leave Aira's face stuck on "explaining" -- report it here instead.
        var isTyping by remember(message.stableKey) { mutableStateOf(false) }
        val currentOnTypingStateChange by rememberUpdatedState(onTypingStateChange)
        DisposableEffect(message.stableKey) {
            onDispose { if (isTyping) currentOnTypingStateChange(false) }
        }

        var visible by remember(message.stableKey) { mutableStateOf(!message.isNew) }
        LaunchedEffect(message.stableKey) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250))
        ) {
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

                Column(modifier = Modifier.widthIn(max = maxWidth)) {
                    if (isThinking) {
                        Box(
                            modifier = Modifier.height(28.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            ThinkingIndicator()
                        }
                    } else {
                        TypewriterText(
                            text = message.text,
                            resetKey = message.stableKey,
                            startRevealed = !message.isNew,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            // Long-press copies the reply
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .longPressToCopy(message.text),
                            growWithReveal = true,
                            onTypingStateChange = { typing ->
                                isTyping = typing
                                onTypingStateChange(typing)
                            }
                        )
                    }
                }
            }
        }
    } else {
        var visible by remember(message.stableKey) { mutableStateOf(!message.isNew) }
        LaunchedEffect(message.stableKey) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250))
        ) {
            // At most 78% of the chat's own width, as in Hajime's Sensei chat. (maxWidth above
            // can't be used here: it's the window width in pixels taken as dp, so it never limits.)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val bubbleMaxWidth = this.maxWidth * 0.78f
                val bubbleShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 4.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = bubbleShape,
                        // Long-press copies the question; the ripple keeps the bubble's shape
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .longPressToCopy(message.text, bubbleShape)
                    ) {
                        // Only as wide as its longest line, so a wrapped message leaves no gap on the right.
                        TightText(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}
