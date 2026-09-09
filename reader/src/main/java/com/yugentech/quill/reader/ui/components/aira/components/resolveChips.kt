package com.yugentech.quill.reader.ui.components.aira.components

import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import org.readium.r2.shared.publication.Locator

fun resolveChips(
    selectedText: String?,
    currentChapterIndex: Int,
    selectedTextLocator: Locator? = null
): List<Pair<String, QuickPrompt>> {
    if (selectedText.isNullOrBlank()) {
        return listOf(
            "Summarize chapter" to QuickPrompt.SummarizeChapter(currentChapterIndex),
            "Who are the characters?" to QuickPrompt.WhoAreTheCharacters,
            "What are the themes?" to QuickPrompt.WhatAreTheThemes(currentChapterIndex)
        )
    }

    val trimmed = selectedText.trim()
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val wordCount = words.size
    val looksLikeProperNoun = isCharacterCandidate(words.first())

    return when (wordCount) {
        1 -> buildList {
            val word = words.first()
            add("Define" to QuickPrompt.DefineWord(word))
            add("What is this?" to QuickPrompt.WhatIsThis(word))
            if (looksLikeProperNoun) {
                add("Who is this?" to QuickPrompt.RecallCharacter(word))
                add("Recent Role" to QuickPrompt.RecentRole(word))
                add("Journey So Far" to QuickPrompt.JourneySoFar(word, currentChapterIndex))
            }
        }
        in 2..3 -> buildList {
            add("Explain this" to QuickPrompt.ExplainThis(trimmed))
            if (looksLikeProperNoun) {
                add("Who is this?" to QuickPrompt.RecallCharacter(trimmed))
                add("Recent Role" to QuickPrompt.RecentRole(trimmed))
                add("Journey So Far" to QuickPrompt.JourneySoFar(trimmed, currentChapterIndex))
            }
        }
        else -> listOf(
            "Explain this" to QuickPrompt.ExplainThis(trimmed),
            "What's the significance?" to QuickPrompt.WhatIsTheSignificance(trimmed, currentChapterIndex),
            "Visualize this" to QuickPrompt.VisualizeThis(
                trimmed,
                currentChapterIndex,
                selectedTextLocator?.toJSON()?.toString()
            )
        )
    }
}

// A capital letter alone doesn't mean a name -- any word opening a sentence is capitalized.
// Pronouns and common sentence-openers are excluded so the character chips (which search the
// book for the literal word) aren't offered for "She", "The", etc., where that search can't work.
private fun isCharacterCandidate(word: String): Boolean {
    val cleaned = word.trim { !it.isLetter() }
    if (cleaned.isEmpty() || !cleaned.first().isUpperCase()) return false
    return cleaned.lowercase() !in NON_NAME_CAPITALIZED_WORDS
}

private val NON_NAME_CAPITALIZED_WORDS = setOf(
    // Pronouns
    "i", "me", "my", "mine", "myself", "you", "your", "yours", "yourself",
    "he", "him", "his", "himself", "she", "her", "hers", "herself",
    "it", "its", "itself", "we", "us", "our", "ours", "ourselves",
    "they", "them", "their", "theirs", "themselves",
    "who", "whom", "whose", "which", "what", "this", "that", "these", "those",
    // Common sentence openers
    "a", "an", "the", "and", "but", "or", "so", "yet", "nor", "for",
    "if", "then", "when", "while", "where", "why", "how", "as", "at", "by", "in", "on", "of",
    "to", "from", "with", "without", "after", "before", "there", "here", "now", "yes", "no",
    "not", "all", "some", "every", "each", "one", "oh", "well", "still", "just", "even"
)