package com.yugentech.quill.ui.tabs.moreScreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.domain.model.NotificationConfig
import com.yugentech.quill.notification.ScheduledNotificationManager
import com.yugentech.quill.user.datastore.UserDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationViewModel(
    private val notificationManager: ScheduledNotificationManager,
    private val userDataStore: UserDataStore,
) : ViewModel() {

    val notificationConfig: StateFlow<NotificationConfig> =
        userDataStore.settingsConfiguration.map { it.notificationConfig }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NotificationConfig()
            )

    private val _showExactAlarmDialog = MutableStateFlow(value = false)
    val showExactAlarmDialog = _showExactAlarmDialog.asStateFlow()

    fun dismissPermissionDialog() {
        _showExactAlarmDialog.value = false
    }

    fun canEnableReminders(): Boolean {
        val hasPermission = notificationManager.canScheduleExactAlarms()
        if (!hasPermission) {
            _showExactAlarmDialog.value = true
            return false
        }
        return true
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setNotificationsEnabled(enabled)
            if (!enabled) {
                notificationManager.cancelReminders()
            } else if (notificationConfig.value.readingRemindersEnabled) {
                updateAlarms()
            }
        }
    }

    fun setReadingRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setReadingRemindersEnabled(enabled)
            if (enabled) {
                if (notificationConfig.value.notificationsEnabled) {
                    updateAlarms()
                }
            } else {
                notificationManager.cancelReminders()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userDataStore.setReminderTime(hour, minute)
            userDataStore.setReadingRemindersEnabled(true)
            if (notificationConfig.value.notificationsEnabled) {
                updateAlarms()
            }
        }
    }

    fun formatReminderTime(): String {
        val config = notificationConfig.value
        if (!config.readingRemindersEnabled) {
            return "Get daily nudges to keep your streak"
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.reminderTimeHour)
            set(Calendar.MINUTE, config.reminderTimeMinute)
        }

        return "Daily at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
    }

    private fun updateAlarms() {
        val config = notificationConfig.value
        try {
            notificationManager.scheduleReminder(config.reminderTimeHour, config.reminderTimeMinute)
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm permission missing")
            _showExactAlarmDialog.value = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule reminder")
        }
    }
}
