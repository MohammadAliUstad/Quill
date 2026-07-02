package com.yugentech.quill.domain.model

data class NotificationConfig(
    val notificationsEnabled: Boolean = true,
    val readingRemindersEnabled: Boolean = false,
    val playfulRemindersEnabled: Boolean = false,
    val reminderTimeHour: Int = 20,
    val reminderTimeMinute: Int = 0
)

data class SettingsConfiguration(
    val hapticsEnabled: Boolean = true,
    val notificationConfig: NotificationConfig = NotificationConfig()
)
