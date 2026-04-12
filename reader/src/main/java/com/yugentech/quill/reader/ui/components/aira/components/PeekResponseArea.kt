package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.quick.state.QuickUiState

private val idleMessages = listOf(
    "Need a quick recap of the this chapter? Just ask.",
    "Highlight any text in the book, or just ask me a question right here!",
    "Want me to translate a phrase or define a tricky word?",
    "Lost in the plot? I can help you find your way.",
    "Need a quick refresher on a character? Just tell me their name."
)

@Composable
fun PeekResponseArea(
    airaUiState: QuickUiState,
    showLimitReached: Boolean,
    selectedText: String? = null,
    activeChips: List<Pair<String, QuickPrompt>> = emptyList(),
    onChipClick: (QuickPrompt) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(350, easing = FastOutSlowInEasing))
    ) {
        when {
            airaUiState.isLoading -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    ThinkingIndicator(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                }
            }

            showLimitReached -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "I'm out of energy for today! You've reached your daily AI query limit. Upgrade to Pro to keep chatting, or I'll see you tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            airaUiState.isStreaming || (selectedText.isNullOrBlank() && (airaUiState.error != null || airaUiState.response != null)) -> {
                val fullText = airaUiState.error ?: airaUiState.response ?: ""
                val textLength = remember { Animatable(0f) }

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
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            !selectedText.isNullOrBlank() -> {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "\"$selectedText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (activeChips.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = activeChips,
                                key = { (label, _) -> label }
                            ) { (label, intent) ->
                                SuggestionChip(
                                    onClick = { onChipClick(intent) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                val greeting = remember { idleMessages.random() }

                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    if (activeChips.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = activeChips,
                                key = { (label, _) -> label }
                            ) { (label, intent) ->
                                SuggestionChip(
                                    onClick = { onChipClick(intent) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}