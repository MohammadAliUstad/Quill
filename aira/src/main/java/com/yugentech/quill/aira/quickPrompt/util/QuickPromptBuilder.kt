package com.yugentech.quill.aira.quickPrompt.util

object QuickPromptBuilder {

    fun whoAreTheCharacters(title: String, author: String, passages: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nBased only on the passages provided, list the key characters the reader has encountered so far.\nFor each, give their name and one brief sentence describing who they are.\nUse plain text only. No markdown, no bold.",
        contextLabel = "PASSAGES",
        context = passages,
        question = "Who are the characters encountered so far?"
    )

    fun whatAreTheThemes(title: String, author: String, passages: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nBased only on the passages provided, identify the main themes present in what the reader has read so far.\nKeep the answer to 3-4 sentences.\nUse plain text only. No markdown, no bold, no headers.",
        contextLabel = "PASSAGES",
        context = passages,
        question = "What are the themes so far?"
    )

    fun whoIsThis(title: String, author: String, name: String, passages: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nBased ONLY on the passages provided, explain who \"$name\" is.\nDescribe their role, personality, or relationship to other characters as shown in what the reader has read so far.\nDo NOT reveal anything beyond what the passages contain. Keep it to 3-4 sentences.\nUse plain text only. No markdown, no bold.",
        contextLabel = "PASSAGES",
        context = passages,
        question = "Who is $name?"
    )

    fun whatIsTheSignificance(
        title: String,
        author: String,
        highlightedText: String,
        surroundingContext: String
    ): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nBased on the highlighted passage and the surrounding context provided, explain why this moment is significant to the story — its impact on characters, plot, or themes.\nKeep it to 3-4 sentences. Plain text only.",
        contextLabel = "CONTEXT PASSAGES",
        context = "HIGHLIGHTED:\n$highlightedText\n\nSURROUNDING CONTEXT:\n$surroundingContext",
        question = "Why is this passage significant?"
    )

    fun whoIsSpeaking(
        title: String,
        author: String,
        highlightedText: String,
        surroundingContext: String
    ): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nBased on the highlighted passage and surrounding context, identify who is speaking or narrating.\nIf it is dialogue, say who said it and to whom. If it is narration, say whose perspective it is.\nKeep it to 2-3 sentences. Use plain text only.",
        contextLabel = "CONTEXT PASSAGES",
        context = "HIGHLIGHTED:\n$highlightedText\n\nSURROUNDING CONTEXT:\n$surroundingContext",
        question = "Who is speaking in this passage?"
    )


    fun summarizeChapter(title: String, author: String, chapterText: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nSummarize the following chapter text in 3-4 sentences.\nFocus on the key events, character actions, and any important revelations.\nUse plain text only. No markdown, no bold, no headers.",
        contextLabel = "CHAPTER TEXT",
        context = chapterText,
        question = "Summarize this chapter."
    )

    fun defineWord(title: String, author: String, word: String): String = assemble(
        system = "You are a precise dictionary. Give a concise definition of the word provided.\nInclude the part of speech and one example sentence if helpful.\nKeep it to 2-3 sentences maximum. Use plain text only.",
        contextLabel = "BOOK CONTEXT",
        context = "This word appears in \"$title\" by $author.",
        question = "Define the word: $word"
    )

    fun whatIsThis(title: String, author: String, word: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nExplain what this word or term refers to — it may be a place, a dish, an object, a social custom, or a historical concept from the book's era.\nKeep the explanation to 2-3 sentences. Use plain text only.",
        contextLabel = "BOOK CONTEXT",
        context = "This term appears in \"$title\" by $author.",
        question = "What is \"$word\"?"
    )

    fun simplifyThis(title: String, author: String, text: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nRewrite the following selection in clear, plain modern English.\nPreserve the meaning exactly — just make it easier to understand.\nDo not summarize or shorten it significantly. Use plain text only.",
        contextLabel = "SELECTED TEXT",
        context = text,
        question = "Rewrite this in plain modern English."
    )

    fun explainThis(title: String, author: String, text: String): String = assemble(
        system = "You are Aira, a reading companion for \"$title\" by $author.\nExplain what the following selection means in context.\nWhat is being said or implied? What literary or historical context is relevant?\nKeep it to 3-4 sentences. Use plain text only.",
        contextLabel = "SELECTED TEXT",
        context = text,
        question = "What does this mean?"
    )


    fun buildContextBlock(texts: List<String>): String =
        if (texts.isEmpty()) "(No relevant passages found.)"
        else texts.joinToString(separator = "\n\n---\n\n")

    private fun assemble(
        system: String,
        contextLabel: String,
        context: String,
        question: String
    ): String = """
        $system
        
        $contextLabel:
        $context
        
        QUESTION:
        $question
    """.trimIndent()
}