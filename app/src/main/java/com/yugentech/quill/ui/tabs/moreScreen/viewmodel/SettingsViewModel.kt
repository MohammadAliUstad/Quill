package com.yugentech.quill.ui.tabs.moreScreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.user.datastore.UserDataStore
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userDataStore: UserDataStore,
    private val hapticService: HapticService
) : ViewModel() {

    val hapticsEnabled: Flow<Boolean> = userDataStore.settingsConfiguration.map { it.hapticsEnabled }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setHapticsEnabled(enabled)
            if (enabled) {
                hapticService.performHaptic()
            }
        }
    }
}
