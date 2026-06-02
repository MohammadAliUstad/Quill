package com.yugentech.quill.aira.aira.util

import com.yugentech.quill.database.model.RetrievedChunk

object AiraBuilder {

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

    fun buildContextBlock(chunks: List<RetrievedChunk>): String =
        if (chunks.isEmpty()) {
            "(No relevant passages found.)"
        } else {
            chunks.mapIndexed { index, chunk ->
                "[ID: $index]\n${chunk.text}"
            }.joinToString(separator = "\n\n---\n\n")
        }
}
