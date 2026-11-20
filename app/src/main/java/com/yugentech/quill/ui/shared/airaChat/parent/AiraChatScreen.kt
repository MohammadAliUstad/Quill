package com.yugentech.quill.ui.shared.airaChat.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.ui.shared.airaChat.viewmodel.AiraViewModel
import com.yugentech.quill.ui.shared.airaChat.components.AiraChatHistory
import com.yugentech.quill.ui.shared.airaChat.components.AiraEmptyState
import com.yugentech.quill.ui.shared.airaChat.components.AiraResetDialog
import com.yugentech.quill.ui.shared.airaChat.components.InputBar
import com.yugentech.quill.ui.shared.airaChat.components.QuotaLimitBar
import com.yugentech.quill.ui.shared.airaChat.components.StatusBanner
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

// Space between the bottom of the chat and the text field.
// The same space as under a reply in the reader's peek bar (its ReplyBottomGap)
private val ChatInputGap = 16.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiraChatScreen(
    viewModel: AiraViewModel,
    onBackClick: () -> Unit,
    navigateToSubscriptions: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    // The limit bar takes the input bar's place when the screen opens out of queries, or when a
    // question is sent with none left. It isn't put up the moment the last one is used, so the
    // answer to it finishes with its stop button. It goes as soon as queries are back: Pro
    // bought, Play confirming Pro, or a new day.
    var enforceLimitUi by remember { mutableStateOf(!uiState.canSendQuery) }
    LaunchedEffect(uiState.canSendQuery) {
        if (uiState.canSendQuery) enforceLimitUi = false
    }

    val isChatEmpty = uiState.messages.isEmpty()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // Seeded near the input bar's real height so the first frame, before it is measured, is close.
    var bottomBarHeightPx by remember { mutableIntStateOf(with(density) { 76.dp.roundToPx() }) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AiraResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.clearChat()
                showResetDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            text = uiState.bookTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = uiState.bookAuthor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isChatEmpty) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            showResetDialog = true
                        }) {
                            Icon(Icons.Default.History, stringResource(R.string.reset_chat))
                        }
                    }
                    // Hidden until Aira has read the whole book (or while it waits its turn) and the
                    // reader has read some of it: there's nothing to lock yet.
                    if (uiState.canChat) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(
                                if (uiState.spoilerLockEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                            )
                            viewModel.toggleSpoilerLock()
                        }) {
                            Icon(
                                imageVector = if (uiState.spoilerLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = stringResource(R.string.toggle_spoiler_lock),
                                tint = if (uiState.spoilerLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
            // The bottom bar's height is measured without the keyboard inset (both the chat and
            // the bar already get that from imePadding). InputBar pads its field down from its own
            // top by lerp(0, 4dp) as the keyboard opens, so that much of the bar sits above the
            // field and is taken back off to keep the gap measured to the field itself.
            val fieldTopInset = lerp(0.dp, 4.dp, kbFraction)
            val bottomClearance = with(density) { bottomBarHeightPx.toDp() } - fieldTopInset + ChatInputGap

            // The book, its index status and the chat history load asynchronously. Showing the
            // defaults before they land would flash the indexing state (or the empty state) for a
            // frame or two, so the content only comes in once all three have answered.
            AnimatedVisibility(
                visible = uiState.isInitialized,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = isChatEmpty,
                    transitionSpec = {
                        if (targetState) {
                            (fadeIn(animationSpec = tween(300)) + slideInVertically(
                                animationSpec = tween(300)
                            ) { it / 8 }) togetherWith fadeOut(animationSpec = tween(300))
                        } else {
                            (fadeIn(animationSpec = tween(400)) + slideInVertically(
                                animationSpec = tween(400)
                            ) { it / 4 }) togetherWith fadeOut(animationSpec = tween(200))
                        }
                    },
                    label = "ChatStateTransition"
                ) { empty ->
                    if (empty) {
                        AiraEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding(),
                            lastChapterTitle = uiState.lastChapterTitle,
                            spoilerLockEnabled = uiState.spoilerLockEnabled,
                            isIndexing = uiState.isIndexing,
                            isQueued = uiState.isQueued,
                            isReady = uiState.isReady,
                            indexingProgress = uiState.indexingProgress,
                            hasStartedReading = uiState.hasStartedReading,
                            onToggleSpoilerLock = { viewModel.toggleSpoilerLock() }
                        )
                    } else {
                        AiraChatHistory(
                            uiState = uiState,
                            listState = listState,
                            bottomPadding = bottomClearance,
                            modifier = Modifier.imePadding()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // The last error, so the banner keeps its text while it slides away
                var shownError by remember { mutableStateOf("") }
                uiState.error?.let { shownError = it }
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    StatusBanner(
                        error = shownError,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            AnimatedContent(
                targetState = enforceLimitUi,
                transitionSpec = { fadeThrough() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                label = "BottomAreaTransition"
            ) { isLimitReached ->
                if (!isLimitReached) {
                    AnimatedVisibility(
                        // Only once Aira can answer: her index of the book is finished and the
                        // reader has started it. Never while it's being read or waiting its turn.
                        visible = uiState.canChat,
                        modifier = Modifier
                            .imePadding()
                            .fillMaxWidth(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        InputBar(
                            modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height },
                            inputText = inputText,
                            onInputChange = { inputText = it },
                            isEnabled = !uiState.isLoading && uiState.isReady,
                            isStreaming = uiState.isStreaming || uiState.isLoading,
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    if (!uiState.canSendQuery) {
                                        enforceLimitUi = true
                                    } else {
                                        viewModel.ask(inputText)
                                        inputText = EMPTY
                                    }
                                }
                            },
                            onStop = {
                                viewModel.stopGeneration()
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height }) {
                        QuotaLimitBar(
                            isPro = uiState.isPro,
                            onUpgradeClick = { navigateToSubscriptions() }
                        )
                    }
                }
            }
        }
    }
}
