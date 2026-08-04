package com.yugentech.quill.reader.ui.components.overlay.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.state.QuickUiState
import com.yugentech.quill.reader.model.BackgroundSound
import com.yugentech.quill.reader.ui.components.aira.AiraPeekBar
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.ReaderBottomControls
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button.AskAiraButton
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button.SoundToggleButton
import com.yugentech.quill.reader.ui.components.overlay.components.topBar.ReaderTopBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPro: Boolean,
    showBottomControls: Boolean = true,
    showAiraPeek: Boolean = false,
    readerOverlayState: ReaderOverlayState,
    airaUiState: QuickUiState = QuickUiState(),
    currentSound: BackgroundSound = BackgroundSound.NONE,
    lastSelectedSound: BackgroundSound = BackgroundSound.RAIN,
    onAction: (ReaderAction) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(readerOverlayState.progress) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragging) {
        if (isDragging) onAction(ReaderAction.OnScrubStart) else onAction(ReaderAction.OnScrubEnd)
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

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ReaderTopBar(
                isVisible = isVisible && !showAiraPeek,
                bookTitle = readerOverlayState.bookTitle,
                onBackClick = { onAction(ReaderAction.OnBackClick) },
                onTocClick = { onAction(ReaderAction.OnTocClick) },
                onSoundClick = { onAction(ReaderAction.OnSoundClick) },
                onSettingsClick = { onAction(ReaderAction.OnSettingsClick) }
            )
        }

        AiraPeekBar(
            isVisible = showAiraPeek,
            selectedText = readerOverlayState.selectedText,
            currentChapterIndex = readerOverlayState.currentChapterIndex,
            airaUiState = airaUiState,
            onQuickAction = { onAction(ReaderAction.OnQuickAction(it)) },
            onSendMessage = { onAction(ReaderAction.OnAiraSend(it)) },
            onDismiss = { onAction(ReaderAction.OnAiraDismiss) },
            onStop = { onAction(ReaderAction.OnStopGeneration) },
            onClearSelection = { onAction(ReaderAction.OnClearSelection) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = isVisible && showBottomControls && airaUiState.isReady,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SoundToggleButton(
                        currentSound = currentSound,
                        lastSelectedSound = lastSelectedSound,
                        onClick = { onAction(ReaderAction.OnSoundQuickToggle) }
                    )
                    AskAiraButton(onClick = { onAction(ReaderAction.OnAskAiraClick) })
                }
            }

            ReaderBottomControls(
                isVisible = isVisible && showBottomControls,
                readerOverlayState = readerOverlayState,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                onSeek = { onAction(ReaderAction.OnSeek(it)) }
            )
        }
    }
}