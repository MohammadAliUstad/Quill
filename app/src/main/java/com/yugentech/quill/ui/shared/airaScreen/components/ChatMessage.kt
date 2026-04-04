package com.yugentech.quill.ui.shared.airaScreen.components

data class ChatMessage(
    val text: String,
    val isFromAira: Boolean,
    val isNew: Boolean = false,
    val stableKey: String = text
)