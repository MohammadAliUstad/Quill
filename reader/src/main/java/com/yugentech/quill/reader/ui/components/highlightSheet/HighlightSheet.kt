package com.yugentech.quill.reader.ui.components.highlightSheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (colorInt: Int) -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0.dp else 28.dp,
        label = "annotateSheetCornerRadius"
    )

    val defaultColors = remember {
        listOf(
            Color(0xFFFFE082), // Amber
            Color(0xFFA5D6A7), // Green
            Color(0xFF90CAF9), // Blue
            Color(0xFFCE93D8), // Purple
            Color(0xFFFF8A80), // Coral
        )
    }

    var selectedColorInt by remember { mutableIntStateOf(defaultColors[0].toArgb()) }
    val selectedColor = remember(selectedColorInt) {
        defaultColors.find { it.toArgb() == selectedColorInt } ?: defaultColors[0]
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("The sky above the port was ")
                        withStyle(SpanStyle(background = selectedColor.copy(alpha = 0.5f))) {
                            append("the color of television")
                        }
                        append(", tuned to a dead channel.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Choose a color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                defaultColors.forEach { color ->
                    HighlightColorSwatch(
                        color = color,
                        isSelected = selectedColorInt == color.toArgb(),
                        onClick = { selectedColorInt = color.toArgb() }
                    )
                }
            }

            Button(
                onClick = { onSave(selectedColorInt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save Highlight")
            }
        }
    }
}

@Composable
private fun HighlightColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val ringSize by animateDpAsState(
        targetValue = if (isSelected) 58.dp else 52.dp,
        animationSpec = spring(),
        label = "swatchRingSize"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        label = "swatchRingAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(ringSize)
                .clip(CircleShape)
                .border(width = 2.dp, color = color.copy(alpha = ringAlpha), shape = CircleShape)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring()) + fadeIn()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.6f) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
