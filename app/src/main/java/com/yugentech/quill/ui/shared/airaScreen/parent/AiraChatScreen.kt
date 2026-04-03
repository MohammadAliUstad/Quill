package com.yugentech.quill.ui.shared.airaScreen.parent

import com.yugentech.quill.ui.shared.airaScreen.components.StatusBanner
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.ui.shared.airaScreen.components.AiraChatHistory
import com.yugentech.quill.ui.shared.airaScreen.components.AiraEmptyState
import com.yugentech.quill.ui.shared.airaScreen.components.InputBar
import com.yugentech.quill.ui.shared.airaScreen.components.QuotaLimitBar
import com.yugentech.quill.ui.shared.airaScreen.components.AiraResetDialog
import com.yugentech.theme.tokens.AppConstants.EMPTY

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiraChatScreen(
    viewModel: AiraViewModel,
    onBackClick: () -> Unit,
    navigateToSubscriptions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isChatEmpty = uiState.messages.isEmpty()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showResetDialog by remember { mutableStateOf(false) }

    // --- Externalized Dialog ---
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
                    IconButton(onClick = { viewModel.toggleSpoilerLock() }) {
                        Icon(
                            imageVector = if (uiState.spoilerLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Spoiler Lock",
                            tint = if (uiState.spoilerLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            val bottomClearance = lerp(100.dp, 76.dp, kbFraction)

            // Chat content
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                AnimatedVisibility(
                    visible = uiState.isIndexing || uiState.error != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    StatusBanner(
                        isIndexing = uiState.isIndexing,
                        indexingProgress = uiState.indexingProgress, // <-- ADD THIS LINE
                        error = uiState.error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            AnimatedContent(
                targetState = uiState.canSendQuery,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                label = "BottomAreaTransition"
            ) { canSend ->
                if (canSend) {
                    AnimatedVisibility(
                        visible = !uiState.isIndexing && uiState.hasStartedReading,
                        modifier = Modifier
                            .imePadding()
                            .fillMaxWidth(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        InputBar(
                            inputText = inputText,
                            onInputChange = { inputText = it },
                            isEnabled = !uiState.isLoading && uiState.isReady,
                            isStreaming = uiState.isStreaming || uiState.isLoading,
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.ask(inputText)
                                    inputText = EMPTY
                                }
                            },
                            onStop = { viewModel.stopGeneration() }
                        )
                    }
                } else {
                    QuotaLimitBar(
                        isPro = uiState.isPro,
                        onUpgradeClick = { navigateToSubscriptions() }
                    )
                }
            }
        }
    }
}