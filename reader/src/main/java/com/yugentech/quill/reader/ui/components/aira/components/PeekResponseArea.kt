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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.state.PeekState

@Composable
fun PeekResponseArea(
    peekState: PeekState,
    contentAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(350, easing = FastOutSlowInEasing))
            .graphicsLayer { alpha = contentAlpha }
    ) {
        when (peekState) {
            is PeekState.Idle -> {}

            is PeekState.Loading -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    ThinkingIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(16.dp))
                }
            }

            is PeekState.Response -> {
                val fullText = peekState.text

                // Keyed on the text itself — resets the counter whenever a new response arrives
                val textLength = remember(fullText) {
                    Animatable(0f)
                }

                LaunchedEffect(fullText) {
                    val charsRemaining = fullText.length - textLength.value
                    if (charsRemaining > 0) {
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
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}