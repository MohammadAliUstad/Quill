package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.QuickActionUiState
import com.yugentech.quill.aira.aira.AiraUiState
import com.yugentech.quill.aira.aira.QuickIntent
import com.yugentech.quill.reader.state.PeekState

@Composable
fun AiraPeekBarContent(
    peekState: PeekState,
    airaUiState: AiraUiState,
    quickActionUiState: QuickActionUiState,
    activeChips: List<Pair<String, QuickIntent>>,
    inputText: String,
    onInputChange: (String) -> Unit,
    canSend: Boolean,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    horizontalPadding: Dp,
    liftDp: Dp,
    contentAlpha: Float,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onQuickAction: (QuickIntent) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = liftDp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Quick action chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = activeChips,
                key = { (label, _) -> label }
            ) { (label, intent) ->
                SuggestionChip(
                    onClick = { onQuickAction(intent) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 20.dp, bottom = 4.dp)
            ) {
                AiraPeekHeader(
                    isLoading = peekState is PeekState.Loading || airaUiState.isLoading,
                    isStreaming = airaUiState.isStreaming,
                    onDismiss = onDismiss
                )

                // Response area
                PeekResponseArea(
                    peekState = peekState,
                    contentAlpha = contentAlpha
                )

                Spacer(Modifier.height(4.dp))

                // Input row
                PeekInputRow(
                    inputText = inputText,
                    onInputChange = onInputChange,
                    airaUiState = airaUiState,
                    quickActionUiState = quickActionUiState,
                    canSend = canSend,
                    buttonContainerColor = buttonContainerColor,
                    buttonContentColor = buttonContentColor,
                    horizontalPadding = horizontalPadding,
                    focusRequester = focusRequester,
                    onFocusChanged = onFocusChanged,
                    onSend = onSend,
                    onStop = onStop
                )
            }
        }
    }
}