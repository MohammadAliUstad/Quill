package com.yugentech.quill.aira.util

import com.yugentech.quill.database.model.RetrievedChunk

object AiraBuilder {

    fun buildContextBlock(chunks: List<RetrievedChunk>): String =
        if (chunks.isEmpty()) {
            "(No relevant passages found.)"
        } else {
            chunks.mapIndexed { index, chunk ->
                "[ID: $index]\n${chunk.text}"
            }.joinToString(separator = "\n\n---\n\n")
        }
}
