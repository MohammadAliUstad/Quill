package com.yugentech.quill.reader.ui.components.overlay.bottomBar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.reader.ui.components.overlay.bottomBar.components.button.AskAiraButton
import com.yugentech.quill.reader.ui.components.overlay.bottomBar.components.sheet.ReaderProgressSheet
import com.yugentech.quill.reader.state.ReaderOverlayState

@Composable
fun ReaderBottomControls(
    isVisible: Boolean,
    readerOverlayState: ReaderOverlayState,
    sliderPosition: Float,
    interactionSource: MutableInteractionSource,
    currentPage: Int,
    onSeek: (Float) -> Unit,
    onAskAiraClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
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
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.End
        ) {
            AskAiraButton(onClick = onAskAiraClick)

            ReaderProgressSheet(
                readerOverlayState = readerOverlayState,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                onSeek = onSeek
            )
        }
    }
}