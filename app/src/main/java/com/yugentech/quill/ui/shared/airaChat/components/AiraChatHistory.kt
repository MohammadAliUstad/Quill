package com.yugentech.quill.ui.shared.airaChat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.message.AiraMessage
import com.yugentech.quill.ui.shared.airaChat.state.AiraUiState

@Composable
fun AiraChatHistory(
    uiState: AiraUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onSpeakClick: (String) -> Unit = {}
) {
    var isVisuallyTyping by remember { mutableStateOf(false) }

    // The reply being generated, remembered from while it's still loading. The reply arrives in
    // one piece and loading/streaming can both clear before the next frame, so by the time its
    // text is first composed those flags may already say it's not new; this keeps it new until
    // its reveal finishes.
    var liveReplyKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.isLoading, uiState.messages.size) {
        if (uiState.isLoading) {
            liveReplyKey = "${uiState.messages.size}_${AiraMessage.Role.AIRA}"
        } else if (uiState.messages.isEmpty()) {
            liveReplyKey = null
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 100.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = uiState.messages.reversed(),
                key = { index, message ->
                    val absoluteIndex = uiState.messages.size - index
                    "${absoluteIndex}_${message.role}"
                }
            ) { index, message ->
                val isAira = message.role == AiraMessage.Role.AIRA
                val stableKey = "${uiState.messages.size - index}_${message.role}"
                val isLiveGeneration = index == 0 && isAira &&
                    (uiState.isLoading || uiState.isStreaming || stableKey == liveReplyKey)
                val shouldRender = message.content.isNotBlank() || isLiveGeneration

                if (shouldRender) {
                    MessageItem(
                        message = ChatMessage(
                            text = message.content,
                            isFromAira = isAira,
                            isNew = isLiveGeneration,
                            stableKey = stableKey
                        ),
                        onTypingStateChange = { isTyping ->
                            if (index == 0 && isAira) {
                                isVisuallyTyping = isTyping
                            }
                            if (!isTyping && stableKey == liveReplyKey) liveReplyKey = null
                        },
                        onSpeakClick = onSpeakClick
                    )
                }
            }
        }

        AiraChatHeader(
            isStreaming = uiState.isStreaming,
            isLoading = uiState.isLoading,
            isVisuallyTyping = isVisuallyTyping,
            lastChapterTitle = uiState.lastChapterTitle,
            spoilerLockEnabled = uiState.spoilerLockEnabled,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
