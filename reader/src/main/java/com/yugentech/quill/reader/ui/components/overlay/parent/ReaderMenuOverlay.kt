package com.yugentech.quill.reader.ui.components.overlay.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.quick.state.QuickUiState
import com.yugentech.quill.reader.ui.components.aira.AiraPeekBar
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.ReaderBottomControls
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button.AskAiraButton
import com.yugentech.quill.reader.ui.components.overlay.components.brightnessSlider.BrightnessSlider
import com.yugentech.quill.reader.ui.components.overlay.components.topBar.ReaderTopBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    isVisible: Boolean,
    isPro: Boolean,
    showBottomControls: Boolean = true,
    showAiraPeek: Boolean = false,
    readerOverlayState: ReaderOverlayState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTocClick: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    onAskAiraClick: () -> Unit = {},
    onAiraDismiss: () -> Unit = {},
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBrightnessInteraction: (Boolean) -> Unit = {},
    onQuickAction: (QuickPrompt) -> Unit = {},
    onAiraSend: (String) -> Unit = {},
    airaUiState: QuickUiState = QuickUiState()
) {
    var sliderPosition by remember { mutableFloatStateOf(readerOverlayState.progress) }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragging) {
        if (isDragging) onScrubStart() else onScrubEnd()
    }

    LaunchedEffect(readerOverlayState.progress) {
        if (!isDragging) sliderPosition = readerOverlayState.progress
    }

    val currentPage = remember(sliderPosition, readerOverlayState.totalPages) {
        ((sliderPosition * (readerOverlayState.totalPages - 1)).roundToInt()).coerceIn(
            0,
            readerOverlayState.totalPages - 1
        ) + 1
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                isVisible = isVisible && !showAiraPeek,
                bookTitle = readerOverlayState.bookTitle,
                onBackClick = onBackClick,
                onTocClick = onTocClick,
                onSettingsClick = onSettingsClick
            )
        }

        BrightnessSlider(
            isVisible = isVisible && !showAiraPeek,
            onDragStart = { onBrightnessInteraction(true) },
            onDragEnd = { onBrightnessInteraction(false) },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        AiraPeekBar(
            isVisible = showAiraPeek,
            selectedText = readerOverlayState.selectedText,
            currentChapterIndex = readerOverlayState.currentChapterIndex,
            airaUiState = airaUiState,
            onQuickAction = onQuickAction,
            onSendMessage = onAiraSend,
            onDismiss = onAiraDismiss,
            onStop = onStop,
            onClearSelection = onClearSelection
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = isVisible && showBottomControls && airaUiState.isReady && isPro,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut()
            ) {
                AskAiraButton(onClick = onAskAiraClick)
            }

            ReaderBottomControls(
                isVisible = isVisible && showBottomControls,
                readerOverlayState = readerOverlayState,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                onSeek = onSeek
            )
        }
    }
}