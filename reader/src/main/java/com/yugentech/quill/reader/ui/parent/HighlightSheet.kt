package com.yugentech.quill.reader.ui.parent

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
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
            Color(0xFF757575),
            Color(0xFFF5F5F5),
            Color(0xFFE2C275),
            Color(0xFF790000),
            Color(0xFF3949AB)
        )
    }

    var selectedColorInt by remember { mutableIntStateOf(defaultColors[0].toArgb()) }

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
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                defaultColors.forEach { color ->
                    val colorInt = color.toArgb()
                    val isSelected = selectedColorInt == colorInt
                    val circleSize by animateDpAsState(targetValue = if (isSelected) 56.dp else 48.dp, label = "sizeAnim")
                    val borderWidth by animateDpAsState(targetValue = if (isSelected) 3.dp else 1.dp, label = "borderAnim")

                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(circleSize)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = borderWidth,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorInt = colorInt }
                        ) {
                            if (isSelected) {
                                val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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

fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}