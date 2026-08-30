package com.yugentech.quill.aira.chat.quickChat.util

import com.yugentech.quill.database.model.RetrievedChunk

object QuickBuilder {
    fun buildContextBlock(chunks: List<RetrievedChunk>): String =
        if (chunks.isEmpty()) "(No relevant passages found.)"
        else chunks.joinToString(separator = "\n\n---\n\n") { chunk ->
            "[Chapter ${chunk.chapterIndex}: ${chunk.chapterTitle}]\n${chunk.text}"
        }
}
