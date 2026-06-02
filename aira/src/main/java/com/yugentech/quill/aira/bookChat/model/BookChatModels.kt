package com.yugentech.quill.aira.aira.bookChat.model

data class BookChatPayload(
    val prompt: String,
    val context: String,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "prompt" to prompt,
            "context" to context,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history
        )
    }
}
