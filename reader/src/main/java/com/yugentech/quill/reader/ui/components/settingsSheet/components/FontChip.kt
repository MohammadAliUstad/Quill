package com.yugentech.quill.reader.ui.components.settingsSheet.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.navigator.preferences.FontFamily as R2FontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun FontChip(
    label: String,
    currentPrefs: EpubPreferences,
    targetFont: R2FontFamily,
    onChange: (EpubPreferences) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    FilterChip(
        selected = currentPrefs.fontFamily == targetFont,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
            onChange(currentPrefs.copy(fontFamily = targetFont))
        },
        label = { Text(label, fontWeight = FontWeight.Medium) },
        shape = CircleShape
    )
}
