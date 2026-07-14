package com.yugentech.quill.reader.ui.components.settingsSheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.reader.pref.model.QuillPreferences
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import com.yugentech.quill.reader.ui.components.settingsSheet.components.CustomSettingsSlider
import com.yugentech.quill.reader.ui.components.settingsSheet.components.FontChip
import com.yugentech.quill.reader.ui.components.settingsSheet.components.SectionLabel
import com.yugentech.quill.reader.ui.components.settingsSheet.components.SettingsSwitchItem
import com.yugentech.quill.reader.ui.components.settingsSheet.components.ThemeOption
import com.yugentech.quill.reader.ui.components.settingsSheet.components.ThemePreset
import com.yugentech.theme.service.HapticService
import com.yugentech.theme.tokens.spacing
import org.koin.compose.koinInject
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import java.util.Locale
import kotlin.math.roundToInt
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.TextAlign as R2TextAlign

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalReadiumApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SettingsSheet(
    preferences: QuillPreferences,
    onPreferencesChange: (EpubPreferences) -> Unit,
    onVolumeNavigationChange: (Boolean) -> Unit,
    onNightLightChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    val epub = preferences.epub
    val fontOptions = remember {
        listOf(
            "Literata" to ReaderDefaults.FONT_LITERATA,
            "Goudy" to ReaderDefaults.FONT_GOUDY,
            "Garamond" to ReaderDefaults.FONT_GARAMOND,
            "Google Sans" to ReaderDefaults.FONT_GOOGLE_SANS,
            "Serif" to ReaderDefaults.FONT_SERIF
        )
    }

    val themePresets = remember {
        listOf(
            ThemePreset(
                name = "Light",
                theme = Theme.LIGHT,
                bgColorInt = 0xFFFAF9F6.toInt(),
                textColorInt = 0xFF2B2B2B.toInt(),
                displayColor = Color(0xFFFAF9F6)
            ),
            ThemePreset(
                name = "Dark",
                theme = Theme.DARK,
                bgColorInt = 0xFF1A1A1A.toInt(),
                textColorInt = 0xFFCECECE.toInt(),
                displayColor = Color(0xFF1A1A1A),
                isDarkBorder = true
            ),
            ThemePreset(
                name = "Night",
                theme = Theme.DARK,
                bgColorInt = 0xFF000000.toInt(),
                textColorInt = 0xFFA0A0A0.toInt(),
                displayColor = Color(0xFF000000),
                isDarkBorder = true
            ),
            ThemePreset(
                name = "Sepia",
                theme = Theme.SEPIA,
                bgColorInt = 0xFFF8E6C8.toInt(),
                textColorInt = 0xFF5A4634.toInt(),
                displayColor = Color(0xFFF8E6C8)
            )
        )
    }

    var activeSlider by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    val isAnyDragging = activeSlider != null

    val sheetBgColor by animateColorAsState(
        targetValue = if (isAnyDragging) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "sheetBgColor"
    )
    val scrimColor by animateColorAsState(
        targetValue = if (isAnyDragging) Color.Transparent else BottomSheetDefaults.ScrimColor,
        label = "scrimColor"
    )
    val nonSliderAlpha by animateFloatAsState(
        targetValue = if (isAnyDragging) 0f else 1f,
        label = "nonSliderAlpha"
    )

    val onDraggingChanged: (String, Boolean) -> Unit = { label, isDragged ->
        if (isDragged) activeSlider = label
        else if (activeSlider == label) activeSlider = null
    }

    val sheetState = rememberModalBottomSheetState()

    val cornerRadius by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0.dp else 28.dp,
        label = "sheetCornerRadius"
    )

    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
            modifier = Modifier.fillMaxHeight(),
            containerColor = sheetBgColor,
            scrimColor = scrimColor,
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        text = "Display Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha }
                    )
                }

                // --- READING MODE (Paged vs Scroll) ---
                item {
                    Column(modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha }) {
                        SectionLabel(text = "Reading Mode")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val isScroll = epub.scroll ?: false
                            SegmentedButton(
                                selected = !isScroll,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(scroll = false))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("Paged") }
                            SegmentedButton(
                                selected = isScroll,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(scroll = true))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("Scroll") }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha }) {
                        SectionLabel(text = "Theme")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            themePresets.forEach { preset ->
                                val isSelected = epub.theme == preset.theme &&
                                        epub.backgroundColor?.int == preset.bgColorInt &&
                                        epub.textColor?.int == preset.textColorInt

                                ThemeOption(
                                    modifier = Modifier.weight(1f),
                                    color = preset.displayColor,
                                    label = preset.name,
                                    isSelected = isSelected,
                                    useLightBorder = preset.isDarkBorder
                                ) {
                                    haptic.performHaptic()
                                    onPreferencesChange(
                                        epub.copy(
                                            theme = preset.theme,
                                            backgroundColor = preset.bgColorInt?.let {
                                                ReadiumColor(
                                                    it
                                                )
                                            },
                                            textColor = preset.textColorInt?.let { ReadiumColor(it) }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha }) {
                        SectionLabel(text = "Font Family")
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(fontOptions) { (label, font) ->
                                FontChip(
                                    label = label,
                                    currentPrefs = epub,
                                    targetFont = font,
                                    onChange = onPreferencesChange
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha }) {
                        SectionLabel("Alignment")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val currentAlign = epub.textAlign ?: R2TextAlign.START

                            SegmentedButton(
                                selected = currentAlign == R2TextAlign.LEFT || currentAlign == R2TextAlign.START,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(textAlign = R2TextAlign.LEFT))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                            ) { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, "Left") }

                            SegmentedButton(
                                selected = currentAlign == R2TextAlign.CENTER,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(textAlign = R2TextAlign.CENTER))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                            ) { Icon(Icons.Default.FormatAlignCenter, "Center") }

                            SegmentedButton(
                                selected = currentAlign == R2TextAlign.JUSTIFY,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(textAlign = R2TextAlign.JUSTIFY))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                            ) { Icon(Icons.Default.FormatAlignJustify, "Justified") }

                            SegmentedButton(
                                selected = currentAlign == R2TextAlign.RIGHT,
                                onClick = {
                                    haptic.performHaptic()
                                    onPreferencesChange(epub.copy(textAlign = R2TextAlign.RIGHT))
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                            ) { Icon(Icons.AutoMirrored.Filled.FormatAlignRight, "Right") }
                        }
                    }
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentFontSize = (epub.fontSize ?: 1.15).toFloat()
                        val displayFontSize =
                            10 + ((currentFontSize - 0.55f) / 0.15f).roundToInt() * 2

                        CustomSettingsSlider(
                            label = "Font Size",
                            value = currentFontSize,
                            valueString = "$displayFontSize",
                            onValueChange = { onPreferencesChange(epub.copy(fontSize = it.toDouble())) },
                            valueRange = 0.55f..2.65f,
                            steps = 13,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged,
                            leadingContent = {
                                Text(
                                    "A",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Text(
                                    "A",
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        val currentFontWeight = (epub.fontWeight ?: 1.0).toFloat()
                        CustomSettingsSlider(
                            label = "Font Weight",
                            value = currentFontWeight,
                            valueString = String.format(Locale.US, "%.1fx", currentFontWeight),
                            onValueChange = { onPreferencesChange(epub.copy(fontWeight = it.toDouble())) },
                            valueRange = 0.5f..2.5f,
                            steps = 9,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged
                        )

                        val currentLineHeight = (epub.lineHeight ?: 1.5).toFloat()
                        CustomSettingsSlider(
                            label = "Line Spacing",
                            value = currentLineHeight,
                            valueString = String.format(Locale.US, "%.2fx", currentLineHeight),
                            onValueChange = { onPreferencesChange(epub.copy(lineHeight = it.toDouble())) },
                            valueRange = 1.0f..2.5f,
                            steps = 5,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged
                        )

                        val currentMargin = (epub.pageMargins ?: 1.0).toFloat()
                        CustomSettingsSlider(
                            label = "Page Margins",
                            value = currentMargin,
                            valueString = String.format(Locale.US, "%.2fx", currentMargin),
                            onValueChange = { onPreferencesChange(epub.copy(pageMargins = it.toDouble())) },
                            valueRange = 0.5f..2.5f,
                            steps = 7,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged
                        )

                        val currentWordSpacing = (epub.wordSpacing ?: 0.0).toFloat()
                        CustomSettingsSlider(
                            label = "Word Spacing",
                            value = currentWordSpacing,
                            valueString = if (currentWordSpacing == 0f) "Default"
                            else String.format(Locale.US, "+%.2fx", currentWordSpacing),
                            onValueChange = { onPreferencesChange(epub.copy(wordSpacing = it.toDouble())) },
                            valueRange = 0.0f..1.0f,
                            steps = 3,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged
                        )

                        val currentLetterSpacing = (epub.letterSpacing ?: 0.0).toFloat()
                        CustomSettingsSlider(
                            label = "Letter Spacing",
                            value = currentLetterSpacing,
                            valueString = if (currentLetterSpacing == 0f) "Default"
                            else String.format(Locale.US, "+%.2fx", currentLetterSpacing),
                            onValueChange = { onPreferencesChange(epub.copy(letterSpacing = it.toDouble())) },
                            valueRange = 0.0f..0.5f,
                            steps = 4,
                            activeSlider = activeSlider,
                            onDraggingChanged = onDraggingChanged
                        )
                    }
                }

                // --- SYSTEM & ADVANCED SETTINGS ---
                item {
                    Column(
                        modifier = Modifier.graphicsLayer { alpha = nonSliderAlpha },
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
                    ) {
                        SectionLabel("System & Advanced")

                        SettingsSwitchItem(
                            title = "Night Light",
                            subtitle = "Warm amber tint for eye comfort",
                            checked = preferences.nightLight,
                            index = 0,
                            totalCount = 2,
                            onCheckedChange = onNightLightChange
                        )

                        SettingsSwitchItem(
                            title = "Volume Button Navigation",
                            subtitle = "Turn pages using physical volume keys",
                            checked = preferences.volumeNavigation,
                            index = 1,
                            totalCount = 2,
                            onCheckedChange = onVolumeNavigationChange
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = nonSliderAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            onClick = {
                                haptic.performHaptic()
                                showResetDialog = true
                            }) {
                            Text("Reset to Defaults")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Reset to Defaults") },
            text = { Text("Are you sure you want to restore all reading settings to their original configuration?") },
            confirmButton = {
                TextButton(onClick = {
                    onPreferencesChange(ReaderDefaults.getPreferences())
                    onVolumeNavigationChange(false)
                    onNightLightChange(false)
                    showResetDialog = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}