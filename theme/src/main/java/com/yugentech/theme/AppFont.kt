package com.yugentech.theme

import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

enum class AppFont(
    val id: String,
    val displayName: String,
    @param:FontRes val regularResId: Int
) {
    Google(
        id = "google",
        displayName = "Google Sans",
        regularResId = R.font.google_sans_flex
    ),
    Outfit(
        id = "outfit",
        displayName = "Outfit",
        regularResId = R.font.outfit
    ),
    Manrope(
        id = "manrope",
        displayName = "Manrope",
        regularResId = R.font.manrope
    ),
    Urbanist(
        id = "urbanist",
        displayName = "Urbanist",
        regularResId = R.font.urbanist
    ),
    Figtree(
        id = "figtree",
        displayName = "Figtree",
        regularResId = R.font.figtree
    )
}

val WindSongFont = FontFamily(
    Font(resId = R.font.windsong)
)