package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.state.QuickUiState

private val loadingPlaceholders = listOf(
    "Finding the right words…",
    "Your answer is taking shape…",
    "Something thoughtful is coming…",
    "Distilling an answer…",
    "Almost there…"
)

@Composable
fun InputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    airaUiState: QuickUiState,
    canSend: Boolean,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    horizontalPadding: Dp,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    val isStreamingOrLoading = airaUiState.isStreaming || airaUiState.isLoading
    val loadingPlaceholder = remember(airaUiState.isLoading) {
        if (airaUiState.isLoading) loadingPlaceholders.random() else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = inputText,
            onValueChange = onInputChange,
            enabled = !airaUiState.isLoading,
            placeholder = {
                Text(
                    text = if (!airaUiState.isLoading) "Ask me anything…" else loadingPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .offset(y = (-2).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✦",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isStreamingOrLoading) MaterialTheme.colorScheme.errorContainer
                            else buttonContainerColor
                        )
                        .clickable(enabled = canSend || isStreamingOrLoading) {
                            if (isStreamingOrLoading) onStop() else onSend(inputText)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = isStreamingOrLoading, label = "SendStopIcon") { loading ->
                        Icon(
                            imageVector = if (loading) Icons.Default.Stop else Icons.Default.ArrowUpward,
                            contentDescription = if (loading) "Stop" else "Send",
                            tint = if (loading) MaterialTheme.colorScheme.onErrorContainer else buttonContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .animateContentSize(),
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            minLines = 1
        )
    }
}
