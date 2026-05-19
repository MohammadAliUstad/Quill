package com.yugentech.quill.reader.pref.model

import org.readium.r2.navigator.epub.EpubPreferences

data class QuillPreferences(
    val epub: EpubPreferences,
    val volumeNavigation: Boolean = false,
    val nightLight: Boolean = false
)