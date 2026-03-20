package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.aira.QuickActionUiState
import com.yugentech.quill.aira.aira.AiraUiState

@Composable
fun PeekInputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    airaUiState: AiraUiState,
    quickActionUiState: QuickActionUiState,
    canSend: Boolean,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    horizontalPadding: Dp,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    val isStreamingOrLoading =
        airaUiState.isStreaming || airaUiState.isLoading || quickActionUiState.isLoading

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
                    text = if (!airaUiState.isLoading) "Ask Aira anything…" else "Aira is thinking…",
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
                FilledIconButton(
                    onClick = { if (isStreamingOrLoading) onStop() else onSend(inputText) },
                    enabled = canSend || isStreamingOrLoading,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isStreamingOrLoading)
                            MaterialTheme.colorScheme.errorContainer else buttonContainerColor,
                        contentColor = if (isStreamingOrLoading)
                            MaterialTheme.colorScheme.onErrorContainer else buttonContentColor
                    )
                ) {
                    AnimatedContent(targetState = isStreamingOrLoading) { loading ->
                        if (loading) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .animateContentSize(),
            shape = RoundedCornerShape(28.dp),
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