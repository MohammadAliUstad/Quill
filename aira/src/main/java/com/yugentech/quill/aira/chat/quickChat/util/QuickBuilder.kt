package com.yugentech.quill.aira.chat.quickChat.util

object QuickBuilder {
    fun buildContextBlock(texts: List<String>): String =
        if (texts.isEmpty()) "(No relevant passages found.)"
        else texts.joinToString(separator = "\n\n---\n\n")
}