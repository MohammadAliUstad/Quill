package com.yugentech.theme.models

import android.os.Parcelable
import com.yugentech.theme.getters.AppFont
import kotlinx.parcelize.Parcelize

@Parcelize
data class ThemeConfiguration(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.CANYON,
    val useDynamicColors: Boolean = false,
    val isAmoledMode: Boolean = false,
    val appFont: AppFont = AppFont.Google
) : Parcelable