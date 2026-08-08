package com.yugentech.quill.reader.ui.components.aira.components

import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt

fun resolveChips(
    selectedText: String?,
    currentChapterIndex: Int
): List<Pair<String, QuickPrompt>> {
    if (selectedText.isNullOrBlank()) {
        return listOf(
            "Summarize chapter" to QuickPrompt.SummarizeChapter(currentChapterIndex),
            "Who are the characters?" to QuickPrompt.WhoAreTheCharacters,
            "What are the themes?" to QuickPrompt.WhatAreTheThemes
        )
    }

    val trimmed = selectedText.trim()
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val wordCount = words.size
    val looksLikeProperNoun = words.first().first().isUpperCase()

    return when (wordCount) {
        1 -> buildList {
            val word = words.first()
            add("Define" to QuickPrompt.DefineWord(word))
            add("What is this?" to QuickPrompt.WhatIsThis(word))
            if (word.first().isUpperCase()) {
                add("Who is this?" to QuickPrompt.WhoIsThis(word, currentChapterIndex))
            }
        }
        in 2..3 -> buildList {
            add("Explain this" to QuickPrompt.ExplainThis(trimmed))
            if (looksLikeProperNoun) {
                add("Who is this?" to QuickPrompt.WhoIsThis(trimmed, currentChapterIndex))
            }
        }
        else -> listOf(
            "Simplify this" to QuickPrompt.SimplifyThis(trimmed),
            "Explain this" to QuickPrompt.ExplainThis(trimmed),
            "What's the significance?" to QuickPrompt.WhatIsTheSignificance(trimmed),
            "Who's speaking?" to QuickPrompt.WhoIsSpeaking(trimmed)
        )
    }
}