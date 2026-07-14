package com.yugentech.quill.reader.ui.components.settingsSheet.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettingsSlider(
    label: String,
    value: Float,
    valueString: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    activeSlider: String?,
    onDraggingChanged: (String, Boolean) -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val haptic = koinInject<HapticService>()
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged) {
        onDraggingChanged(label, isDragged)
    }

    val isAnyDragging = activeSlider != null
    val isThisActive = activeSlider == label

    val alpha by animateFloatAsState(
        targetValue = if (isAnyDragging && !isThisActive) 0f else 1f,
        label = "sliderAlpha"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isThisActive) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        label = "sliderBg"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isThisActive) 16.dp else 0.dp,
        label = "sliderHorizontalPadding"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueString,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) leadingContent()
            Slider(
                value = value,
                onValueChange = {
                    if (it != value) {
                        haptic.performHaptic()
                    }
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (leadingContent != null || trailingContent != null) 8.dp else 0.dp)
            )
            if (trailingContent != null) trailingContent()
        }
    }
}