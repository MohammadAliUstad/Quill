package com.yugentech.theme

import com.yugentech.theme.models.ThemeConfiguration
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeConfiguration: Flow<ThemeConfiguration>
    suspend fun setThemeConfig(config: ThemeConfiguration)
    suspend fun resetThemeToDefaults()
}