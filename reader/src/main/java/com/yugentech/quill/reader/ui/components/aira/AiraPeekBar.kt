package com.yugentech.quill.reader.ui.components.aira

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.reader.ui.components.aira.components.AiraPeekHeader
import com.yugentech.quill.reader.ui.components.aira.components.InputBar
import com.yugentech.quill.reader.ui.components.aira.components.PeekResponseArea
import com.yugentech.quill.reader.ui.components.aira.components.QuotaLimitBar
import com.yugentech.quill.reader.ui.components.aira.components.resolveChips
import com.yugentech.quill.reader.viewmodel.ReaderAiraUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiraPeekBar(
    isVisible: Boolean,
    selectedText: String? = null,
    currentChapterIndex: Int = 0,
    airaUiState: ReaderAiraUiState,
    onQuickAction: (QuickPrompt) -> Unit,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    onUpgradeClick: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }

    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val activeChips = remember(selectedText, currentChapterIndex) {
        resolveChips(selectedText, currentChapterIndex)
    }

    LaunchedEffect(selectedText) { inputText = selectedText ?: "" }
    LaunchedEffect(isVisible) { inputText = if (isVisible) selectedText ?: "" else "" }
    LaunchedEffect(isImeVisible) { if (!isImeVisible) focusManager.clearFocus() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liftDp = (imeBottom - navBottom).coerceAtLeast(0.dp)
    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
    val horizontalPadding = lerp(24.dp, 8.dp, kbFraction)

    val canSend = inputText.isNotBlank() && !airaUiState.isLoading

    val buttonContainerColor by animateColorAsState(
        targetValue = if (canSend) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContainerColor"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (canSend) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContentColor"
    )

    fun send(text: String) {
        if (text.isBlank()) return
        onSendMessage(text)
        inputText = ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(380, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = liftDp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Only show chips if they have quota
                AnimatedVisibility(visible = airaUiState.canSendQuery) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .offset(y = 4.dp)
                            .zIndex(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = activeChips,
                            key = { (label, _) -> label }
                        ) { (label, intent) ->
                            SuggestionChip(
                                onClick = {
                                    onQuickAction(intent)
                                    inputText = ""
                                    focusManager.clearFocus()
                                },
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
                            .padding(top = 12.dp, bottom = 4.dp)
                    ) {
                        AiraPeekHeader(
                            isLoading = airaUiState.isLoading,
                            onDismiss = onDismiss
                        )

                        PeekResponseArea(
                            airaUiState = airaUiState
                        )

                        // Smoothly animate between the InputBar and QuotaLimitBar
                        AnimatedContent(
                            targetState = airaUiState.canSendQuery,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "InputBarSwap"
                        ) { hasQuota ->
                            if (hasQuota) {
                                InputBar(
                                    inputText = inputText,
                                    onInputChange = { inputText = it },
                                    airaUiState = airaUiState,
                                    canSend = canSend,
                                    buttonContainerColor = buttonContainerColor,
                                    buttonContentColor = buttonContentColor,
                                    horizontalPadding = horizontalPadding,
                                    focusRequester = focusRequester,
                                    onFocusChanged = { isFocused = it },
                                    onSend = ::send,
                                    onStop = onStop
                                )
                            } else {
                                QuotaLimitBar(onUpgradeClick = onUpgradeClick)
                            }
                        }
                    }
                }
            }
        }
    }
}