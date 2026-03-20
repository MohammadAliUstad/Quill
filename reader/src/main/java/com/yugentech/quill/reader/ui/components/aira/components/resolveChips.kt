package com.yugentech.quill.reader.ui.components.aira.components

import com.yugentech.quill.aira.aira.QuickIntent

fun resolveChips(
    selectedText: String?,
    currentChapterIndex: Int
): List<Pair<String, QuickIntent>> {
    if (selectedText.isNullOrBlank()) {
        return listOf(
            "Summarize chapter" to QuickIntent.SummarizeChapter(currentChapterIndex),
            "Who are the characters?" to QuickIntent.WhoAreTheCharacters,
            "What are the themes?" to QuickIntent.WhatAreTheThemes
        )
    }

    val trimmed = selectedText.trim()
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val wordCount = words.size
    val looksLikeProperNoun = words.first().first().isUpperCase()

    return when {
        wordCount == 1 -> buildList {
            val word = words.first()
            add("Define" to QuickIntent.DefineWord(word))
            add("What is this?" to QuickIntent.WhatIsThis(word))
            if (word.first().isUpperCase()) {
                add("Who is this?" to QuickIntent.WhoIsThis(word, currentChapterIndex))
            }
        }

        wordCount in 2..3 -> buildList {
            add("Explain this" to QuickIntent.ExplainThis(trimmed))
            if (looksLikeProperNoun) {
                add("Who is this?" to QuickIntent.WhoIsThis(trimmed, currentChapterIndex))
            }
        }

        else -> listOf(
            "Simplify this" to QuickIntent.SimplifyThis(trimmed),
            "Explain this" to QuickIntent.ExplainThis(trimmed),
            "What's the significance?" to QuickIntent.WhatIsTheSignificance(trimmed),
            "Who's speaking?" to QuickIntent.WhoIsSpeaking(trimmed)
        )
    }
}