package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.reader.state.QuickUiState

@Composable
fun PeekResponseArea(
    airaUiState: QuickUiState,
    showLimitReached: Boolean,
    selectedText: String? = null,
    activeChips: List<Pair<String, QuickPrompt>> = emptyList(),
    onChipClick: (QuickPrompt) -> Unit = {},
    onGreetingSelected: (String) -> Unit = {}
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

            airaUiState.error != null -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = airaUiState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            airaUiState.response != null -> {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = airaUiState.response,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            else -> {
                QuickActionChips(
                    selectedText = selectedText,
                    activeChips = activeChips,
                    onChipClick = onChipClick,
                    onGreetingSelected = onGreetingSelected
                )
            }
        }
    }
}

@Composable
private fun QuickActionChips(
    selectedText: String?,
    activeChips: List<Pair<String, QuickPrompt>>,
    onChipClick: (QuickPrompt) -> Unit,
    onGreetingSelected: (String) -> Unit
) {
    val greetings = listOf(
        "How can I help you today?",
        "What's on your mind regarding this book?",
        "Ask me anything about the story!",
        "Want to dive deeper into this chapter?"
    )
    val greeting = remember { greetings.random() }
    LaunchedEffect(Unit) { onGreetingSelected(greeting) }

    Column {
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (selectedText.isNullOrBlank()) greeting else "Analyze Selection",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeChips) { (label, intent) ->
                Surface(
                    onClick = { onChipClick(intent) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
