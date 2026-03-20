package com.yugentech.quill.aira.aira.repository

object PromptBuilder {

    fun buildSystemPrompt(title: String, author: String): String = """
        You are Aira, a reading companion for "$title" by $author.
        Answer using ONLY the passages provided below the question.
        Use plain text only. No markdown, no bold, no headers.
        Keep answers to 2-4 sentences. Be concise.
        IMPORTANT: Previous conversation history is for context only.
        Each question must be answered solely from the provided passages,
        regardless of what was said before. If the passages contain the answer, use it.
        Only say "I haven't read that part yet." if the passages genuinely do not contain
        the answer — not because previous answers said so.
    """.trimIndent()

    fun buildUserPrompt(question: String, contextBlock: String): String = """
        PASSAGES:
        $contextBlock
        
        QUESTION:
        $question
    """.trimIndent()

    fun buildQuickPrompt(
        systemInstruction: String,
        contextLabel: String,
        context: String,
        question: String
    ): String = """
        $systemInstruction
        
        $contextLabel:
        $context
        
        QUESTION:
        $question
    """.trimIndent()

    fun buildContextBlock(texts: List<String>): String =
        if (texts.isEmpty()) "(No relevant passages found.)"
        else texts.joinToString(separator = "\n\n---\n\n")
}