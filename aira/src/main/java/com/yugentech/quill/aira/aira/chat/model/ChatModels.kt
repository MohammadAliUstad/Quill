package com.yugentech.quill.aira.aira.chat.model

enum class ChatRequestType {
    GENERAL,
    BOOK
}

data class ChatPayload(
    val prompt: String,
    val requestType: ChatRequestType,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "prompt" to prompt,
            "requestType" to requestType.name,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history
        )
    }
}
