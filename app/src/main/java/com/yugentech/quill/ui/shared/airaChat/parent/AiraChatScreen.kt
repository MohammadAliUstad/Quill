package com.yugentech.quill.ui.shared.airaChat.parent

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.util.VoiceOutputManager
import com.yugentech.quill.ui.shared.airaChat.viewmodel.AiraViewModel
import com.yugentech.quill.ui.shared.airaChat.components.AiraChatHistory
import com.yugentech.quill.ui.shared.airaChat.components.AiraEmptyState
import com.yugentech.quill.ui.shared.airaChat.components.AiraResetDialog
import com.yugentech.quill.ui.shared.airaChat.components.InputBar
import com.yugentech.quill.ui.shared.airaChat.components.QuotaLimitBar
import com.yugentech.quill.ui.shared.airaChat.components.StatusBanner
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.theme.tokens.dimensions.AppAnimations
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// Space between the bottom of the chat and the text field.
private val ChatInputGap = 8.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiraChatScreen(
    viewModel: AiraViewModel,
    onBackClick: () -> Unit,
    navigateToSubscriptions: () -> Unit
) {
    val voiceOutputManager: VoiceOutputManager = koinInject()
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    var enforceLimitUi by remember { mutableStateOf(!uiState.canSendQuery) }

    DisposableEffect(Unit) {
        onDispose {
            voiceOutputManager.stop()
        }
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
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isChatEmpty) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Default.History, "Reset Chat")
                        }
                    }
                    // Hidden while Aira is still reading the book, or before the reader has read
                    // anything (Aira is locked then): there's nothing to lock yet.
                    if (uiState.isInitialized && !uiState.isIndexing && uiState.hasStartedReading) {
                        IconButton(onClick = { viewModel.toggleSpoilerLock() }) {
                            Icon(
                                imageVector = if (uiState.spoilerLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Spoiler Lock",
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
                            indexingProgress = uiState.indexingProgress,
                            hasStartedReading = uiState.hasStartedReading,
                            onToggleSpoilerLock = { viewModel.toggleSpoilerLock() }
                        )
                    } else {
                        AiraChatHistory(
                            uiState = uiState,
                            listState = listState,
                            bottomPadding = bottomClearance,
                            onSpeakClick = { text ->
                                scope.launch { voiceOutputManager.speak(text) }
                            },
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
                // Errors only: indexing progress now shows around Aira's portrait in the empty state.
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    StatusBanner(
                        isIndexing = false,
                        indexingProgress = uiState.indexingProgress,
                        error = uiState.error,
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
                        visible = uiState.isInitialized && !uiState.isIndexing && uiState.hasStartedReading,
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
