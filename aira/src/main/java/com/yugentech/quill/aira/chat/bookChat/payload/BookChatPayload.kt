package com.yugentech.quill.aira.chat.bookChat.payload

data class BookChatPayload(
    val query: String,
    val context: String,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "query" to query,
            "context" to context,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history
        )
    }
}