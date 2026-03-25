package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.ui.components.aira.PeekState

private val idleMessages = listOf(
    "Need a quick recap of the this chapter? Just ask.",
    "Highlight any text in the book, or just ask me a question right here!",
    "Want me to translate a phrase or define a tricky word?",
    "Lost in the plot? I can help you find your way.",
    "Need a quick refresher on a character? Just tell me their name."
)

@Composable
fun PeekResponseArea(
    peekState: PeekState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(350, easing = FastOutSlowInEasing))
    ) {
        when (peekState) {
            is PeekState.Idle -> {
                val greeting = remember { idleMessages.random() }

                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            is PeekState.Loading -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    ThinkingIndicator(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                }
            }

            is PeekState.Response -> {
                val fullText = peekState.text

                // THE FIX: Removed `fullText` from the remember key.
                // Now, if Aira is streaming text, the Animatable won't reset to 0f on every new word.
                // It naturally resets to 0 only when the state switches from Loading -> Response.
                val textLength = remember {
                    Animatable(0f)
                }

                // Triggers whenever new text is appended to fullText
                LaunchedEffect(fullText) {
                    if (textLength.value < fullText.length) {
                        val charsRemaining = fullText.length - textLength.value
                        textLength.animateTo(
                            targetValue = fullText.length.toFloat(),
                            animationSpec = tween(
                                durationMillis = (charsRemaining * 15f).toInt().coerceAtLeast(10),
                                easing = LinearEasing
                            )
                        )
                    }
                }

                val displayedText = fullText.substring(
                    0, textLength.value.toInt().coerceAtMost(fullText.length)
                )

                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp) // Aligned padding with Idle/Loading states
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}