package com.yugentech.quill.aira.aira.generalChat.model

data class GeneralChatPayload(
    val prompt: String,
    val bookTitle: String,
    val bookAuthor: String,
    val history: List<Map<String, Any>>
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "prompt" to prompt,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "history" to history
        )
    }
}
