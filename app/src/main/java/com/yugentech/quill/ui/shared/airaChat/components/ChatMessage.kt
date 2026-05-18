package com.yugentech.quill.ui.shared.airaChat.components

import com.yugentech.quill.database.model.RetrievedChunk

data class ChatMessage(
    val text: String,
    val isFromAira: Boolean,
    val isNew: Boolean,
    val stableKey: String,
    val sources: List<RetrievedChunk> = emptyList()
)