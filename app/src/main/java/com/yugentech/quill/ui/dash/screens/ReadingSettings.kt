package com.yugentech.quill.ui.dash.screens

data class ReadingSettings(
    val fontSize: Int = 18,
    val fontFamily: FontFamily = FontFamily.SERIF,
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val lineHeight: Float = 1.6f,
    val brightness: Float = 1.0f,
    val pageMargins: Int = 24
)

enum class FontFamily(val cssName: String) {
    SERIF("Georgia, serif"),
    SANS_SERIF("Arial, sans-serif"),
    MONOSPACE("'Courier New', monospace")
}

enum class ReaderTheme(
    val backgroundColor: String,
    val textColor: String,
    val displayName: String
) {
    LIGHT("#FFFFFF", "#000000", "Light"),
    DARK("#1E1E1E", "#E0E0E0", "Dark"),
    SEPIA("#FBF0D9", "#3E2723", "Sepia"),
    BLACK("#000000", "#FFFFFF", "Black")
}