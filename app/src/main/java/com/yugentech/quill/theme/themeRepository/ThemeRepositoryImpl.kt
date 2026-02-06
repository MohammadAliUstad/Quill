package com.yugentech.quill.theme.themeRepository

import com.yugentech.quill.theme.ThemeService
import com.yugentech.quill.theme.models.ThemeConfiguration
import timber.log.Timber

class ThemeRepositoryImpl(
    private val themeService: ThemeService
) : ThemeRepository {

    // Delegate the flow directly to the service layer
    override val themeConfiguration = themeService.themeConfiguration

    override suspend fun setThemeConfig(config: ThemeConfiguration) {
        Timber.i("Updating theme configuration: Mode=${config.themeMode}, Color=${config.colorTheme}")
        // Pass the new configuration to the service for persistence
        themeService.updateThemeConfig(config)
    }

    override suspend fun resetThemeToDefaults() {
        Timber.d("Resetting theme to defaults")
        // Trigger the service to clear custom settings
        themeService.resetToDefaults()
    }
}