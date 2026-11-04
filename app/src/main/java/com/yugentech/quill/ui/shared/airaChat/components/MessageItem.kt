package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.ui.components.aira.components.ThinkingIndicator
import com.yugentech.quill.reader.ui.components.aira.components.TypewriterText

/** How long the Read Aloud / Copy row takes to grow open under a finished reply. */
private const val ACTIONS_ENTER_MS = 320

/** The row's fade starts a little after it begins growing, so the icons appear into open space. */
private const val ACTIONS_FADE_DELAY_MS = 80

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
                            modifier = Modifier.padding(top = 4.dp),
                            growWithReveal = true,
                            onTypingStateChange = { typing ->
                                isTyping = typing
                                onTypingStateChange(typing)
                                if (!typing) isRevealed = true
                            }
                        )

                        // Grows open from under the reply (pushing the chat up the same way the
                        // reply's own lines do) and fades in, rather than popping in at full
                        // height once the reveal ends. Already-revealed messages start visible,
                        // so they don't animate.
                        AnimatedVisibility(
                            visible = isRevealed,
                            enter = expandVertically(
                                animationSpec = tween(ACTIONS_ENTER_MS, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Top
                            ) + fadeIn(tween(ACTIONS_ENTER_MS, delayMillis = ACTIONS_FADE_DELAY_MS))
                        ) {
                            Column {
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
                        modifier = Modifier.widthIn(max = bubbleMaxWidth)
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
