package com.yugentech.quill.reader.ui.overlay.parent

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.QuickActionUiState
import com.yugentech.quill.aira.aira.AiraUiState
import com.yugentech.quill.aira.aira.QuickIntent
import com.yugentech.quill.reader.ui.aira.AiraPeekBar
import com.yugentech.quill.reader.ui.overlay.bottomBar.ReaderBottomControls
import com.yugentech.quill.reader.ui.overlay.brightnessSlider.BrightnessSlider
import com.yugentech.quill.reader.ui.overlay.topBar.ReaderTopBar
import com.yugentech.quill.reader.state.ReaderOverlayState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    isVisible: Boolean,
    showBottomControls: Boolean = true,
    showAiraPeek: Boolean = false,
    readerOverlayState: ReaderOverlayState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTocClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onAskAiraClick: () -> Unit = {},
    onAiraDismiss: () -> Unit = {},
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onBrightnessInteraction: (Boolean) -> Unit = {},
    onQuickAction: (QuickIntent) -> Unit = {},
    onAiraSend: (String) -> Unit = {},
    airaUiState: AiraUiState = AiraUiState(),
    quickActionUiState: QuickActionUiState = QuickActionUiState()
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
        ((sliderPosition * (readerOverlayState.totalPages - 1)).roundToInt()).coerceIn(0, readerOverlayState.totalPages - 1) + 1
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

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomControls(
                isVisible = isVisible && showBottomControls,
                readerOverlayState = readerOverlayState,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                onSeek = onSeek,
                onAskAiraClick = onAskAiraClick
            )
        }

        if (!showAiraPeek) {
            BrightnessSlider(
                isVisible = isVisible,
                onDragStart = { onBrightnessInteraction(true) },
                onDragEnd = { onBrightnessInteraction(false) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

        AiraPeekBar(
            isVisible = showAiraPeek,
            selectedText = readerOverlayState.selectedText,
            currentChapterIndex = readerOverlayState.currentChapterIndex,
            quickActionUiState = quickActionUiState,
            airaUiState = airaUiState,
            onQuickAction = onQuickAction,
            onSendMessage = onAiraSend,
            onDismiss = onAiraDismiss,
        )
    }
}