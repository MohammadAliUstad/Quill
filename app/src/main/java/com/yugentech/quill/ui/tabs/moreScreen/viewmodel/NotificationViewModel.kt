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

    // Set when the user is sent to system settings for the exact-alarm permission, so that
    // coming back with it granted can finish what they were doing instead of making them
    // tap the reminder toggle again.
    private var awaitingExactAlarmPermission = false

    fun dismissPermissionDialog() {
        _showExactAlarmDialog.value = false
    }

    // User backed out of the permission dialog without going to settings.
    fun cancelPermissionRequest() {
        awaitingExactAlarmPermission = false
        _showExactAlarmDialog.value = false
    }

    fun canEnableReminders(): Boolean {
        val hasPermission = notificationManager.canScheduleExactAlarms()
        if (!hasPermission) {
            requestExactAlarmPermission()
            return false
        }
        return true
    }

    // Called when the screen resumes. Returns true if the user just came back from granting
    // the exact-alarm permission while trying to turn the reminder on, meaning the time
    // picker should open to finish that. If the reminder was already on (the alarm just
    // couldn't be scheduled), it's rescheduled here directly instead.
    fun onReturnedFromSettings(): Boolean {
        if (!awaitingExactAlarmPermission || !notificationManager.canScheduleExactAlarms()) return false
        awaitingExactAlarmPermission = false

        val config = notificationConfig.value
        if (config.notificationsEnabled && config.readingRemindersEnabled) {
            updateAlarms(config.reminderTimeHour, config.reminderTimeMinute)
            return false
        }
        return true
    }

    private fun requestExactAlarmPermission() {
        awaitingExactAlarmPermission = true
        _showExactAlarmDialog.value = true
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setNotificationsEnabled(enabled)
            if (!enabled) {
                notificationManager.cancelReminders()
                notificationManager.cancelPlayfulReminders()
            } else {
                val config = notificationConfig.value
                if (config.readingRemindersEnabled) {
                    updateAlarms(config.reminderTimeHour, config.reminderTimeMinute)
                }
                if (notificationConfig.value.playfulRemindersEnabled) {
                    notificationManager.schedulePlayfulReminders()
                }
            }
        }
    }

    fun setReadingRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setReadingRemindersEnabled(enabled)
            if (enabled) {
                val config = notificationConfig.value
                if (config.notificationsEnabled) {
                    updateAlarms(config.reminderTimeHour, config.reminderTimeMinute)
                }
            } else {
                notificationManager.cancelReminders()
            }
        }
    }

    fun setPlayfulRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userDataStore.setPlayfulRemindersEnabled(enabled)
            if (enabled) {
                if (notificationConfig.value.notificationsEnabled) {
                    notificationManager.schedulePlayfulReminders()
                }
            } else {
                notificationManager.cancelPlayfulReminders()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userDataStore.setReminderTime(hour, minute)
            userDataStore.setReadingRemindersEnabled(true)
            // Schedule with the time just picked, not notificationConfig.value -- the saved
            // time only reaches that StateFlow after DataStore emits, so reading it back here
            // can still return the previous time.
            if (notificationConfig.value.notificationsEnabled) {
                updateAlarms(hour, minute)
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

    private fun updateAlarms(hour: Int, minute: Int) {
        try {
            notificationManager.scheduleReminder(hour, minute)
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm permission missing")
            requestExactAlarmPermission()
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule reminder")
        }
    }
}
