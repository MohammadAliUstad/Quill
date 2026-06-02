package com.yugentech.quill.aira.quick.model

enum class QuickActionType {
    SUMMARIZE_CHAPTER,
    WHO_ARE_CHARACTERS,
    WHO_IS_THIS,
    WHAT_ARE_THEES,
    DEFINE_WORD,
    WHAT_IS_THIS,
    SIMPLIFY_THIS,
    EXPLAIN_THIS,
    WHAT_SIGNIFICANCE,
    WHO_IS_SPEAKING
}

data class QuickActionPayload(
    val actionType: QuickActionType,
    val bookTitle: String,
    val bookAuthor: String,
    val context: String,
    val question: String? = null
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "actionType" to actionType.name,
            "bookTitle" to bookTitle,
            "bookAuthor" to bookAuthor,
            "context" to context,
            "question" to (question ?: "")
        )
    }
}
