package com.yugentech.quill.domain.model

// The reminder settings live in the app's notification package (NotificationConfig).
data class SettingsConfiguration(
    val hapticsEnabled: Boolean = true
)
