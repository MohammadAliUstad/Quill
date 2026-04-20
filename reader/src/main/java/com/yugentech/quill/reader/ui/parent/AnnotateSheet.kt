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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotateSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (colorInt: Int, style: String) -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0.dp else 28.dp,
        label = "annotateSheetCornerRadius"
    )

    // Store the native integer color instead of the enum to support custom colors
    var selectedColorInt by remember { mutableIntStateOf(HighlightColor.YELLOW.nativeColor) }
    var selectedStyleIndex by remember { mutableIntStateOf(0) } // 0 = Highlight, 1 = Underline

    // State for the extended color picker
    var showColorPicker by remember { mutableStateOf(false) }
    var customColorInt by remember { mutableStateOf<Int?>(null) }

    val styles = listOf("Highlight", "Underline")

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
                text = "Annotate",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- 1. STYLE SELECTION ---
            Text("Style", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                styles.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                        onClick = { selectedStyleIndex = index },
                        selected = index == selectedStyleIndex
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. COLOR SELECTION ---
            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Render the 4 preset colors
                HighlightColor.entries.forEach { colorEnum ->
                    val isSelected = selectedColorInt == colorEnum.nativeColor
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(colorEnum.nativeColor))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorInt = colorEnum.nativeColor }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (colorEnum == HighlightColor.YELLOW) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Render the 5th "Custom" Color Button
                val isCustomSelected = HighlightColor.entries.none { it.nativeColor == selectedColorInt }

                // Sweep gradient for the rainbow icon
                val rainbowBrush = Brush.sweepGradient(
                    listOf(Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green, Color.Blue, Color(0xFF4B0082), Color(0xFF8B00FF))
                )

                // Conditionally apply background
                val customBgModifier = if (customColorInt == null) {
                    Modifier.background(brush = rainbowBrush, shape = CircleShape)
                } else {
                    Modifier.background(color = Color(customColorInt!!), shape = CircleShape)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .then(customBgModifier)
                        .border(
                            width = if (isCustomSelected) 3.dp else 0.dp,
                            color = if (isCustomSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            if (customColorInt == null) {
                                showColorPicker = true
                            } else if (!isCustomSelected) {
                                selectedColorInt = customColorInt!!
                            } else {
                                showColorPicker = true
                            }
                        }
                ) {
                    if (isCustomSelected) {
                        val checkTint = if (customColorInt != null && Color(customColorInt!!).luminance() > 0.5f) Color.Black else Color.White
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Custom Selected",
                            tint = checkTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 3. SAVE BUTTON ---
            Button(
                onClick = {
                    val styleString = if (selectedStyleIndex == 1) "UNDERLINE" else "HIGHLIGHT"
                    onSave(selectedColorInt, styleString)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save Annotation")
            }
        }
    }

    // --- EXTENDED COLOR PICKER DIALOG ---
    if (showColorPicker) {
        ExtendedColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { newColor ->
                val newColorInt = newColor.toArgb()
                customColorInt = newColorInt
                selectedColorInt = newColorInt
                showColorPicker = false // Instantly close upon selection
            }
        )
    }
}

@Composable
fun ExtendedColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    // A curated list of 16 beautiful material colors
    val extendedColors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFEC407A), // Pink
        Color(0xFFAB47BC), // Purple
        Color(0xFF7E57C2), // Deep Purple
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF42A5F5), // Blue
        Color(0xFF29B6F6), // Light Blue
        Color(0xFF26C6DA), // Cyan
        Color(0xFF26A69A), // Teal
        Color(0xFF66BB6A), // Green
        Color(0xFF9CCC65), // Light Green
        Color(0xFFD4E157), // Lime
        Color(0xFFFFCA28), // Amber
        Color(0xFFFFA726), // Orange
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF8D6E63)  // Brown
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Color", style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(extendedColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Extension function to determine checkmark text color
fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).toDouble()
        .pow(2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}