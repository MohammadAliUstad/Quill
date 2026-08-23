package com.yugentech.quill.reader.ui.components.aira

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.aira.util.VoiceOutputManager
import com.yugentech.quill.reader.state.QuickUiState
import com.yugentech.quill.reader.ui.components.aira.components.AiraPeekHeader
import com.yugentech.quill.reader.ui.components.aira.components.InputBar
import com.yugentech.quill.reader.ui.components.aira.components.PeekResponseArea
import com.yugentech.quill.reader.ui.components.aira.components.QuotaLimitBar
import com.yugentech.quill.reader.ui.components.aira.components.resolveChips
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiraPeekBar(
    isVisible: Boolean,
    selectedText: String? = null,
    currentChapterIndex: Int = 0,
    airaUiState: QuickUiState,
    onQuickAction: (QuickPrompt) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStop: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current
    val voiceOutputManager: VoiceOutputManager = koinInject()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            voiceOutputManager.stop()
        }
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }
    var currentGreeting by remember { mutableStateOf("") }

    val contentToActUpon = remember(airaUiState.response, airaUiState.error, currentGreeting) {
        airaUiState.error ?: airaUiState.response ?: currentGreeting
    }

    val onCopyResponse = {
        if (contentToActUpon.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Aira Content", contentToActUpon)
            clipboard.setPrimaryClip(clip)
        }
    }

    val activeChips = remember(selectedText, currentChapterIndex) {
        resolveChips(selectedText, currentChapterIndex)
    }

    var enforceLimitUi by remember { mutableStateOf(!airaUiState.canSendQuery) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            enforceLimitUi = !airaUiState.canSendQuery
        } else {
            inputText = ""
        }
    }

    LaunchedEffect(airaUiState.canSendQuery) {
        if (!airaUiState.canSendQuery) enforceLimitUi = true
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) { if (!isImeVisible) focusManager.clearFocus() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liftDp = (imeBottom - navBottom).coerceAtLeast(0.dp)
    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
    val horizontalPadding = lerp(16.dp, 8.dp, kbFraction)

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
        focusManager.clearFocus()
        onClearSelection()
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
                            hasResponse = airaUiState.response != null,
                            onDismiss = {
                                haptic.performHaptic(view)
                                onDismiss()
                            },
                            onSpeak = {
                                haptic.performHaptic(view)
                                contentToActUpon.let { text ->
                                    if (text.isNotBlank()) {
                                        scope.launch { voiceOutputManager.speak(text) }
                                    }
                                }
                            },
                            onCopy = {
                                haptic.performHaptic(view)
                                onCopyResponse()
                            }
                        )

                        PeekResponseArea(
                            airaUiState = airaUiState,
                            showLimitReached = enforceLimitUi,
                            selectedText = selectedText,
                            activeChips = activeChips,
                            onChipClick = { intent ->
                                haptic.performHaptic(view)
                                onQuickAction(intent)
                                inputText = ""
                                focusManager.clearFocus()
                                onClearSelection()
                            },
                            onGreetingSelected = { currentGreeting = it }
                        )

                        AnimatedContent(
                            targetState = enforceLimitUi,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                    animationSpec = tween(300)
                                )
                            },
                            label = "InputBarSwap"
                        ) { isLimitReached ->
                            if (!isLimitReached) {
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
                                    onSend = {
                                        haptic.performHaptic(view)
                                        send(it)
                                    },
                                    onStop = {
                                        haptic.performHaptic(view)
                                        onStop()
                                    }
                                )
                            } else {
                                QuotaLimitBar(
                                    isPro = airaUiState.isPro
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
