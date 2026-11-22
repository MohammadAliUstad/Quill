package com.yugentech.quill.theme.themeRepository

import com.yugentech.quill.theme.service.ThemeService
import com.yugentech.theme.ThemeRepository
import com.yugentech.theme.models.ThemeConfiguration

class ThemeRepositoryImpl(
    private val themeService: ThemeService
) : ThemeRepository {

    override val themeConfiguration = themeService.themeConfiguration

    override suspend fun setThemeConfig(config: ThemeConfiguration) {
        themeService.updateThemeConfig(config)
    }

    override suspend fun resetThemeToDefaults() {
        themeService.resetToDefaults()
    }
}