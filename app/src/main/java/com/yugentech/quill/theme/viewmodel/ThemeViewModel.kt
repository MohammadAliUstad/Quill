package com.yugentech.quill.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.theme.AppFont
import com.yugentech.theme.ThemeRepository
import com.yugentech.theme.models.ColorTheme
import com.yugentech.theme.models.ThemeConfiguration
import com.yugentech.theme.models.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _themeConfiguration = MutableStateFlow(
        ThemeConfiguration(
            themeMode = ThemeMode.LIGHT,
            colorTheme = ColorTheme.QUILL,
            useDynamicColors = true,
            isAmoledMode = false,
            appFont = AppFont.Google
        )
    )

    val themeConfiguration: StateFlow<ThemeConfiguration> = _themeConfiguration.asStateFlow()

    val currentFont: StateFlow<AppFont> = _themeConfiguration
        .map { it.appFont }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppFont.Google
        )

    init {
        viewModelScope.launch {
            themeRepository.themeConfiguration.collect { config ->
                _themeConfiguration.value = config
            }
        }
    }

    fun updateTheme(config: ThemeConfiguration) {
        _themeConfiguration.value = config

        viewModelScope.launch {
            themeRepository.setThemeConfig(config)
        }
    }

    fun setFont(font: AppFont) {
        val current = _themeConfiguration.value
        val newConfig = current.copy(appFont = font)
        updateTheme(newConfig)
    }
}