package com.yugentech.quill.aira.aira.util

object LongPromptBuilder {

    private val DEAD_END_PREFIXES = listOf(
        "I haven't read that part yet",
        "I don't have enough information",
        "The passages don't",
        "The passages do not",
        "There is no information",
        "The provided passages"
    )

    fun isDeadEnd(response: String): Boolean {
        val trimmed = response.trim()
        return DEAD_END_PREFIXES.any { prefix -> trimmed.startsWith(prefix, ignoreCase = true) }
    }

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

    fun buildExpansionPrompt(question: String): String = """
        Rewrite the following question into exactly 3 search queries for retrieving passages from a book.
        Rules:
        - Use declarative phrases, not questions
        - Each variant must preserve the original meaning exactly — do not add assumptions or new information
        - Each variant should approach the concept from a genuinely different angle, not just synonym substitution
        - Output exactly 3 lines with no preamble, no numbering, no extra text whatsoever
        
        Question: $question
    """.trimIndent()

    fun buildContextBlock(texts: List<String>): String =
        if (texts.isEmpty()) "(No relevant passages found.)"
        else texts.joinToString(separator = "\n\n---\n\n")
}