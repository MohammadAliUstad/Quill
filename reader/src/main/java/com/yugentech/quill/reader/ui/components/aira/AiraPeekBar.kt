package com.yugentech.quill.reader.ui.aira

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.QuickActionUiState
import com.yugentech.quill.aira.aira.AiraUiState
import com.yugentech.quill.aira.aira.QuickIntent
import com.yugentech.quill.reader.state.PeekState
import com.yugentech.quill.reader.ui.aira.components.AiraPeekHeader
import com.yugentech.quill.reader.ui.aira.components.ThinkingIndicator

private fun resolveChips(
    selectedText: String?,
    currentChapterIndex: Int
): List<Pair<String, QuickIntent>> {
    if (selectedText.isNullOrBlank()) {
        return listOf(
            "Summarize chapter" to QuickIntent.SummarizeChapter(currentChapterIndex),
            "Who are the characters?" to QuickIntent.WhoAreTheCharacters,
            "What are the themes?" to QuickIntent.WhatAreTheThemes
        )
    }

    val trimmed = selectedText.trim()
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val wordCount = words.size
    val looksLikeProperNoun = words.first().first().isUpperCase()

    return when {
        wordCount == 1 -> buildList {
            val word = words.first()
            add("Define" to QuickIntent.DefineWord(word))
            add("What is this?" to QuickIntent.WhatIsThis(word))
            if (word.first().isUpperCase()) {
                add("Who is this?" to QuickIntent.WhoIsThis(word, currentChapterIndex))
            }
        }

        wordCount in 2..3 -> buildList {
            add("Explain this" to QuickIntent.ExplainThis(trimmed))
            if (looksLikeProperNoun) {
                add("Who is this?" to QuickIntent.WhoIsThis(trimmed, currentChapterIndex))
            }
        }

        else -> listOf(
            "Simplify this" to QuickIntent.SimplifyThis(trimmed),
            "Explain this" to QuickIntent.ExplainThis(trimmed),
            "What's the significance?" to QuickIntent.WhatIsTheSignificance(trimmed),
            "Who's speaking?" to QuickIntent.WhoIsSpeaking(trimmed)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiraPeekBar(
    isVisible: Boolean,
    selectedText: String? = null,
    currentChapterIndex: Int = 0,
    quickActionUiState: QuickActionUiState,
    airaUiState: AiraUiState,
    onQuickAction: (QuickIntent) -> Unit,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val activeChips = remember(selectedText, currentChapterIndex) {
        resolveChips(selectedText, currentChapterIndex)
    }

    LaunchedEffect(selectedText) {
        inputText = selectedText ?: ""
    }

    // FIX: Ensure it populates correctly when the bottom sheet slides up
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            inputText = ""
        } else {
            inputText = selectedText ?: ""
        }
    }
    LaunchedEffect(isImeVisible) { if (!isImeVisible) focusManager.clearFocus() }

    // Response area driven by QuickActionUiState only
    // Response area driven by QuickActionUiState only
    val error = quickActionUiState.error
    val response = quickActionUiState.response

    val peekState: PeekState = when {
        quickActionUiState.isLoading -> PeekState.Loading
        // Now the compiler knows `error` and `response` are local and can't change
        error != null -> PeekState.Response(error)
        response != null -> PeekState.Response(response)
        else -> PeekState.Idle
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (peekState is PeekState.Idle) 0f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liftDp = (imeBottom - navBottom).coerceAtLeast(0.dp)
    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
    val horizontalPadding = lerp(24.dp, 8.dp, kbFraction)

    // Send button guarded by airaUiState — manual questions go through AiraViewModel
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    .copy(alpha = 0.92f),
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
                            isLoading = peekState is PeekState.Loading,
                            onDismiss = onDismiss
                        )

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
                                        ThinkingIndicator(
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                    }
                                }

                                is PeekState.Response -> {
                                    Column {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = peekState.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                enabled = !airaUiState.isLoading,
                                placeholder = {
                                    Text(
                                        text = if (!airaUiState.isLoading) "Ask Aira anything…"
                                        else "Aira is thinking…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                            .copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = {
                                    FilledIconButton(
                                        onClick = { send(inputText) },
                                        enabled = canSend,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(36.dp),
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = buttonContainerColor,
                                            contentColor = buttonContentColor,
                                            disabledContainerColor = buttonContainerColor,
                                            disabledContentColor = buttonContentColor
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .animateContentSize(),
                                shape = RoundedCornerShape(28.dp),
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
            }
        }
    }
}