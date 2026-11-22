package com.yugentech.quill.aira.chat.generalChat.payload

import com.yugentech.quill.aira.util.ReaderPosition

data class GeneralChatPayload(
    val query: String,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>,
    val position: ReaderPosition,
    val selectedText: String? = null,
    val userName: String? = null
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf<String, Any>(
            "query" to query,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history,
            "selectedText" to (selectedText ?: ""),
            "userName" to (userName ?: "")
        ).apply { putAll(position.toMap()) }
    }
}
