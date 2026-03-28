package com.yugentech.quill.reader.ui.components.engine

import androidx.core.graphics.toColorInt
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

object ReaderDefaults {
    val FONT_GOOGLE_SANS = FontFamily("Google Sans Flex")
    val FONT_LITERATA = FontFamily("Literata")
    val FONT_GOUDY = FontFamily("Goudy Bookletter 1911")
    val FONT_GARAMOND = FontFamily("Garamond")
    val FONT_SERIF = FontFamily.SERIF
    private val DEFAULT_BG_COLOR = Color("#000000".toColorInt())
    private val DEFAULT_TEXT_COLOR = Color("#A0A0A0".toColorInt())

    @OptIn(ExperimentalReadiumApi::class)
    fun getPreferences(): EpubPreferences = EpubPreferences(
        scroll = true,
        fontFamily = FONT_LITERATA,
        fontSize = 1.15,
        lineHeight = 1.5,
        pageMargins = 1.0,
        wordSpacing = 0.0,
        letterSpacing = 0.0,
        textAlign = TextAlign.LEFT,
        theme = Theme.DARK,
        backgroundColor = DEFAULT_BG_COLOR,
        textColor = DEFAULT_TEXT_COLOR,
        publisherStyles = false
    )
}