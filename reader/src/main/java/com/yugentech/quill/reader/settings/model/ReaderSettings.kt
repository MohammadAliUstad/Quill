package com.yugentech.quill.reader.settings.model

import com.yugentech.quill.reader.sound.model.BackgroundSound
import org.readium.r2.navigator.epub.EpubPreferences

data class ReaderSettings(
    val epub: EpubPreferences,
    val volumeNavigation: Boolean = false,
    val nightLight: Boolean = false,
    val autoPlaySound: Boolean = false,
    val lastSelectedSound: BackgroundSound = BackgroundSound.RAIN,
    val soundVolume: Float = 1.0f
)