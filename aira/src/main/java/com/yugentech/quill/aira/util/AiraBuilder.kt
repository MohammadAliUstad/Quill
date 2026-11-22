package com.yugentech.quill.aira.util

import com.yugentech.quill.database.model.RetrievedChunk

object AiraBuilder {

    /** Each passage labelled with its chapter, so Aira can say where in the book something happens. */
    fun buildContextBlock(chunks: List<RetrievedChunk>): String =
        if (chunks.isEmpty()) {
            "(No relevant passages found.)"
        } else {
            chunks.joinToString(separator = "\n\n---\n\n") { chunk ->
                val title = chunk.chapterTitle.trim()
                if (title.isEmpty()) chunk.text else "[$title]\n${chunk.text}"
            }
        }
}
