package com.yugentech.quill.aira.chat.bookChat.payload

import com.yugentech.quill.aira.util.ReaderPosition

data class BookChatPayload(
    val query: String,
    val context: String,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>,
    val queryIntent: String,
    val position: ReaderPosition,
    val selectedText: String? = null,
    val userName: String? = null
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf<String, Any>(
            "query" to query,
            "context" to context,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history,
            "queryIntent" to queryIntent,
            "selectedText" to (selectedText ?: ""),
            "userName" to (userName ?: "")
        ).apply { putAll(position.toMap()) }
    }
}
