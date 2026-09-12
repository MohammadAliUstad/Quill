package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.ui.components.aira.components.ThinkingIndicator
import com.yugentech.quill.reader.ui.components.aira.components.TypewriterText

@Composable
fun MessageItem(
    message: ChatMessage,
    onTypingStateChange: (Boolean) -> Unit = {},
    onSpeakClick: (String) -> Unit = {}
) {
    val isAira = message.isFromAira
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = (containerWidth * 0.78f).dp
    val clipboardManager = LocalClipboardManager.current

    if (isAira) {
        val isThinking = message.isNew && message.text.isBlank()

        var isRevealed by remember(message.stableKey) { mutableStateOf(!message.isNew) }

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
                            modifier = Modifier.padding(top = 4.dp),
                            onTypingStateChange = { isTyping ->
                                onTypingStateChange(isTyping)
                                if (!isTyping) isRevealed = true
                            }
                        )

                        if (isRevealed) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Right side: Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Speak Button
                                    IconButton(
                                        onClick = { onSpeakClick(message.text) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Copy Button
                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(message.text)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
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
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
