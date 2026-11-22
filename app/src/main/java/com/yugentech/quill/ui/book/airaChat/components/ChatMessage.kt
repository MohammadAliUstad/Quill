package com.yugentech.quill.ui.book.airaChat.components

data class ChatMessage(
    val text: String,
    val isFromAira: Boolean,
    val isNew: Boolean,
    val stableKey: String
)
