package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputBar(
    modifier: Modifier = Modifier,
    inputText: String,
    onInputChange: (String) -> Unit,
    isEnabled: Boolean,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) focusManager.clearFocus()
    }

    val collapsedHPadding = 32.dp
    val expandedHPadding = 8.dp
    val collapsedVPadding = 0.dp
    val expandedVPadding = 4.dp

    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)

    val horizontalPadding = lerp(collapsedHPadding, expandedHPadding, kbFraction)
    val verticalPadding = lerp(collapsedVPadding, expandedVPadding, kbFraction)

    val canSend = inputText.isNotBlank() && isEnabled
    val surfaceColor = MaterialTheme.colorScheme.surface

    val buttonContainerColor by animateColorAsState(
        targetValue = when {
            isStreaming -> MaterialTheme.colorScheme.errorContainer
            canSend -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContainerColor"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = when {
            isStreaming -> MaterialTheme.colorScheme.onErrorContainer
            canSend -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.15f to surfaceColor.copy(alpha = 0.1f),
                        0.35f to surfaceColor.copy(alpha = 0.4f),
                        0.55f to surfaceColor.copy(alpha = 0.7f),
                        0.75f to surfaceColor.copy(alpha = 0.9f),
                        1.00f to surfaceColor
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = inputText,
                onValueChange = { onInputChange(it) },
                enabled = isEnabled,
                placeholder = {
                    Text(
                        text = if (isEnabled) "Ask Aira anything…" else "Aira is thinking…",
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
                            .background(buttonContainerColor)
                            .clickable(enabled = canSend || isStreaming) {
                                if (isStreaming) {
                                    onStop()
                                } else {
                                    onSend()
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isStreaming,
                            label = "SendStopIcon"
                        ) { streaming ->
                            Icon(
                                imageVector = if (streaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                                contentDescription = if (streaming) "Stop" else "Send",
                                tint = buttonContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                minLines = 1,
            )
        }
    }
}
