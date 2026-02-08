package com.yugentech.quill.reader.ui.components.overlay.brightnessSlider

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSlider(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val context = LocalContext.current

    val initialBrightness = remember {
        val window = (context as? Activity)?.window
        val current = window?.attributes?.screenBrightness ?: -1f
        if (current < 0f) 0.5f else current
    }

    val sliderState = rememberSliderState(
        value = initialBrightness,
        valueRange = 0.05f..1f
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragging) {
        if (isDragging) onDragStart() else onDragEnd()
    }

    LaunchedEffect(sliderState.value) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val lp = activity.window.attributes
        lp.screenBrightness = sliderState.value
        activity.window.attributes = lp
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
        exit = fadeOut(tween(200)) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.padding(end = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 20.dp)
            ) {
                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier
                        .height(200.dp)
                        .progressSemantics(sliderState.value, 0.05f..1f, 0),
                    interactionSource = interactionSource,
                    reverseDirection = true,
                    colors = SliderDefaults.colors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedSunIcon(
                    brightness = sliderState.value,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}