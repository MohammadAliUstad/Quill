package com.yugentech.quill.aira.chat.quickChat.model

enum class QuickChatType {
    SUMMARIZE_CHAPTER,
    WHO_ARE_CHARACTERS,
    WHO_IS_THIS,
    WHAT_ARE_THEMES,
    DEFINE_WORD,
    WHAT_IS_THIS,
    SIMPLIFY_THIS,
    EXPLAIN_THIS,
    WHAT_SIGNIFICANCE,
    WHO_IS_SPEAKING
}

data class QuickChatPayload(
    val actionType: QuickChatType,
    val bookTitle: String,
    val bookAuthor: String,
    val context: String,
    val query: String? = null
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "actionType" to actionType.name,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "context" to context,
            "query" to (query ?: "")
        )
    }
}