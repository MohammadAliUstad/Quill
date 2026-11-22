package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.yugentech.quill.reader.viewmodel.quick.QuickUiState
import com.yugentech.theme.effects.airaBeam
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.quill.reader.R
import androidx.compose.ui.res.stringResource

private val loadingPlaceholders = listOf(
    R.string.reader_thinking_1,
    R.string.reader_thinking_2,
    R.string.reader_thinking_3,
    R.string.reader_thinking_4,
    R.string.reader_thinking_5,
    R.string.reader_thinking_6,
    R.string.reader_thinking_7,
    R.string.reader_thinking_8,
    R.string.reader_thinking_9,
    R.string.reader_thinking_10,
    R.string.reader_thinking_11,
    R.string.reader_thinking_12
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
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()
    val placeholderTexts = loadingPlaceholders.map { stringResource(it) }
    var loadingPlaceholder by remember { mutableStateOf("") }
    var lastPlaceholderIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(airaUiState.isLoading) {
        if (airaUiState.isLoading) {
            var index: Int
            do { index = loadingPlaceholders.indices.random() }
            while (index == lastPlaceholderIndex && loadingPlaceholders.size > 1)
            lastPlaceholderIndex = index
            loadingPlaceholder = placeholderTexts[index]
        } else {
            loadingPlaceholder = ""
        }
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
                    text = if (!airaUiState.isLoading) stringResource(R.string.reader_ask_anything) else loadingPlaceholder,
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
                            if (isStreamingOrLoading) MaterialTheme.colorScheme.secondaryContainer
                            else buttonContainerColor
                        )
                        .clickable(enabled = canSend || isStreamingOrLoading) {
                            if (isStreamingOrLoading) onStop() else onSend(inputText)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isStreamingOrLoading,
                        transitionSpec = { fadeThrough() },
                        label = "SendStopIcon"
                    ) { loading ->
                        Icon(
                            imageVector = if (loading) Icons.Default.Stop else Icons.Default.ArrowUpward,
                            contentDescription = if (loading) stringResource(R.string.reader_stop) else stringResource(R.string.reader_send),
                            tint = if (loading) MaterialTheme.colorScheme.onSecondaryContainer else buttonContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                // A light circles the field while Aira is answering (loading and streaming).
                // Ahead of animateContentSize, so it follows the field's size as it eases.
                .airaBeam(active = isStreamingOrLoading)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .animateContentSize(),
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
