package com.yugentech.quill.ui.shared.airaScreen.components

data class ChatMessage(
    val text: String,
    val isFromAira: Boolean,
    val isNew: Boolean = false,
    // Stable unique key — pass message ID from DB, or timestamp
    val stableKey: String = text
)