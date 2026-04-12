package com.yugentech.quill.theme.themeRepository

import com.yugentech.quill.theme.service.ThemeService
import com.yugentech.theme.ThemeRepository
import com.yugentech.theme.models.ThemeConfiguration
import timber.log.Timber

class ThemeRepositoryImpl(
    private val themeService: ThemeService
) : ThemeRepository {

    override val themeConfiguration = themeService.themeConfiguration

    override suspend fun setThemeConfig(config: ThemeConfiguration) {
        Timber.i("Updating theme configuration: Mode=${config.themeMode}, Color=${config.colorTheme}")
        themeService.updateThemeConfig(config)
    }

    override suspend fun resetThemeToDefaults() {
        Timber.d("Resetting theme to defaults")
        themeService.resetToDefaults()
    }
}