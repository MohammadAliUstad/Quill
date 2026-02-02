package com.yugentech.quill.viewModels

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.alerts.models.AlertsConfiguration
import com.yugentech.quill.alerts.alertsRepository.AlertsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    // Exposes current alert settings (sound/haptics) to the UI
    val alertConfigurations: StateFlow<AlertsConfiguration> = alertsRepository.alertConfiguration

    // Updates the sound preference
    fun setSoundEnabled(enabled: Boolean) {
        Timber.d("User toggled sound: $enabled")
        viewModelScope.launch {
            alertsRepository.setSoundEnabled(enabled)
        }
    }

    // Updates the haptics preference
    fun setHapticsEnabled(enabled: Boolean) {
        Timber.d("User toggled haptics: $enabled")
        viewModelScope.launch {
            alertsRepository.setHapticsEnabled(enabled)
        }
    }

    // Triggers a haptic feedback effect immediately
    fun performHaptic(view: View? = null) {
        alertsRepository.performHaptic(view)
    }
}