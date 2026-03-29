package com.yugentech.quill.aira.quickPrompt.state

sealed class QuickPrompt {

    // RAG
    data object WhoAreTheCharacters : QuickPrompt()
    data object WhatAreTheThemes : QuickPrompt()
    data class WhoIsThis(val name: String, val currentChapterIndex: Int) : QuickPrompt()
    data class WhatIsTheSignificance(val text: String) : QuickPrompt()
    data class WhoIsSpeaking(val text: String) : QuickPrompt()

    // NO RAG
    data class SummarizeChapter(val chapterIndex: Int) : QuickPrompt()
    data class DefineWord(val word: String) : QuickPrompt()
    data class WhatIsThis(val word: String) : QuickPrompt()
    data class SimplifyThis(val text: String) : QuickPrompt()
    data class ExplainThis(val text: String) : QuickPrompt()
}