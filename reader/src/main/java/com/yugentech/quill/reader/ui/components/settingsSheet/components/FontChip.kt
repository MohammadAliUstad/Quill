package com.yugentech.quill.reader.ui.components.settingsSheet.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject
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
    val haptic = koinInject<HapticService>()
    val view = LocalView.current

    FilterChip(
        selected = currentPrefs.fontFamily == targetFont,
        onClick = {
            haptic.performHaptic(view)
            onChange(currentPrefs.copy(fontFamily = targetFont))
        },
        label = { Text(label, fontWeight = FontWeight.Medium) },
        shape = CircleShape
    )
}
