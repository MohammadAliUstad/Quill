package com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.sheet

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.sheet.components.BookProgressFooter
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.sheet.components.ChapterProgressHeader
import com.yugentech.quill.reader.ui.components.overlay.parent.ReaderOverlayState
import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderProgressSheet(
    readerOverlayState: ReaderOverlayState,
    sliderPosition: Float,
    interactionSource: MutableInteractionSource,
    currentPage: Int,
    onSeek: (Float) -> Unit
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            ChapterProgressHeader(
                chapterTitle = readerOverlayState.chapterTitle,
                chapterPagesLeft = readerOverlayState.chapterPagesLeft
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Track the last page that triggered a haptic to prevent double-firing
            var lastHapticPage by remember { mutableIntStateOf(currentPage) }
            var draggingValue by remember { mutableFloatStateOf(-1f) }
            val displayValue = if (draggingValue >= 0f) draggingValue else sliderPosition

            Slider(
                value = displayValue,
                onValueChange = { newPos ->
                    draggingValue = newPos
                    val newPage = (newPos * (readerOverlayState.totalPages - 1)).roundToInt() + 1
                    if (newPage != lastHapticPage) {
                        haptic.performTickHaptic(view)
                        lastHapticPage = newPage
                    }
                    onSeek(newPos)
                },
                onValueChangeFinished = { draggingValue = -1f },
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            BookProgressFooter(
                currentPage = currentPage,
                totalPages = readerOverlayState.totalPages,
                sliderPosition = displayValue
            )
        }
    }
}
