package com.yugentech.quill.reader.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.aira.aira.AiraUiState
import com.yugentech.quill.reader.ui.reader.airaInteraction.AiraPeekBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuOverlay(
    isVisible: Boolean,
    showBottomControls: Boolean = true,
    showAiraPeek: Boolean = false,
    bookTitle: String,
    progress: Float,
    totalPages: Int,
    chapterTitle: String,
    chapterPagesLeft: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTocClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onAskAiraClick: () -> Unit = {},
    onAiraDismiss: () -> Unit = {},
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    selectedText: String? = null,
    airaUiState: AiraUiState = AiraUiState(),
    onAiraSend: (String) -> Unit = {},
) {
    var sliderPosition by remember { mutableFloatStateOf(progress) }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragging) {
        if (isDragging) onScrubStart() else onScrubEnd()
    }
    LaunchedEffect(progress) {
        if (!isDragging) sliderPosition = progress
    }

    val currentPage = remember(sliderPosition, totalPages) {
        ((sliderPosition * (totalPages - 1)).roundToInt()).coerceIn(0, totalPages - 1) + 1
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- TOP BAR ---
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ReaderTopBar(
                isVisible = isVisible && !showAiraPeek,
                bookTitle = bookTitle,
                onBackClick = onBackClick,
                onTocClick = onTocClick,
                onSettingsClick = onSettingsClick
            )
        }

        // --- BOTTOM CONTROLS ---
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ReaderBottomControls(
                isVisible = isVisible && showBottomControls,
                chapterTitle = chapterTitle,
                chapterPagesLeft = chapterPagesLeft,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                totalPages = totalPages,
                onSeek = onSeek,
                onAskAiraClick = onAskAiraClick
            )
        }

        // --- AIRA PEEK BAR ---
        AiraPeekBar(
            isVisible = showAiraPeek,
            selectedText = selectedText,
            airaUiState = airaUiState,
            onSendMessage = onAiraSend,
            onDismiss = onAiraDismiss,
        )
    }
}