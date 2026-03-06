package com.yugentech.quill.ui.shared.airaScreen.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.aira.AiraMessage
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.ui.shared.airaScreen.components.AiraChapter
import com.yugentech.quill.ui.shared.airaScreen.components.AiraOptionsDialog
import com.yugentech.quill.ui.shared.airaScreen.components.ChatMessage
import com.yugentech.quill.ui.shared.airaScreen.components.InputBar
import com.yugentech.quill.ui.shared.airaScreen.components.MessageItem
import com.yugentech.quill.ui.shared.airaScreen.components.StatusBanner
import com.yugentech.quill.ui.shared.airaScreen.components.WelcomeState
import com.yugentech.theme.getters.AppFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiraChatScreen(
    bookId: String,
    bookTitle: String = "",
    bookAuthor: String = "",
    chapters: List<AiraChapter> = emptyList(),
    currentChapterHref: String? = null,
    onChapterSelected: (AiraChapter) -> Unit = {},
    onBackClick: () -> Unit,
    viewModel: AiraViewModel
) {
    LaunchedEffect(bookId) {
        if (bookId.isNotBlank()) viewModel.initForBook(bookId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val windSongFont = remember { AppFont.WindSong.toFontFamily() }
    val bodyFontFamily = MaterialTheme.typography.bodyLarge.fontFamily

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty() || uiState.isLoading) {
            listState.animateScrollToItem(0)
        }
    }

    if (showOptionsDialog) {
        AiraOptionsDialog(
            chapters = chapters,
            currentChapterHref = currentChapterHref,
            spoilerLockEnabled = uiState.spoilerLockEnabled,
            onChapterSelected = onChapterSelected,
            onSpoilerLockToggle = { viewModel.toggleSpoilerLock() },
            onDismiss = { showOptionsDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { _ ->

        Box(modifier = Modifier.fillMaxSize()) {

            if (uiState.messages.isEmpty()) {
                WelcomeState(
                    bookTitle = bookTitle,
                    windSongFont = windSongFont,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 100.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.messages.reversed(),
                        key = { index, message ->
                            val absoluteIndex = uiState.messages.size - index
                            "${absoluteIndex}_${message.role}"
                        }
                    ) { index, message ->
                        val isAira = message.role == AiraMessage.Role.AIRA
                        val isLiveGeneration = index == 0 && isAira && (uiState.isLoading || uiState.isStreaming)
                        val shouldRender = message.content.isNotBlank() || isLiveGeneration

                        if (shouldRender) {
                            MessageItem(
                                message = ChatMessage(
                                    text = message.content,
                                    isFromAira = isAira,
                                    isNew = isLiveGeneration,
                                    stableKey = "${uiState.messages.size - index}_${message.role}"
                                ),
                                bodyFontFamily = bodyFontFamily
                            )
                        }
                    }
                }
            }

            // --- GRADIENT TOP BAR ---
            val surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to surfaceColor.copy(alpha = 1.0f),
                                0.3f to surfaceColor.copy(alpha = 0.95f),
                                0.6f to surfaceColor.copy(alpha = 0.75f),
                                0.85f to surfaceColor.copy(alpha = 0.40f),
                                1.0f to Color.Transparent
                            )
                        )
                )
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Column {
                            Text(
                                text = bookTitle.ifBlank { "Aira" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (bookAuthor.isNotBlank()) {
                                Text(
                                    text = bookAuthor,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }

            // --- STATUS BANNER ---
            AnimatedVisibility(
                visible = uiState.isIndexing || uiState.error != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            ) {
                StatusBanner(
                    isIndexing = uiState.isIndexing,
                    error = uiState.error,
                    onDismiss = { viewModel.clearError() }
                )
            }

            // --- INPUT BAR ---
            InputBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                inputText = inputText,
                onInputChange = { inputText = it },
                isEnabled = !uiState.isLoading && uiState.isReady,
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.ask(inputText)
                        inputText = ""
                    }
                },
                onOptionsClick = { showOptionsDialog = true }
            )
        }
    }
}