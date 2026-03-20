package com.yugentech.quill.aira.aira.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.yugentech.quill.aira.aira.AiraResponse
import com.yugentech.quill.aira.aira.QuickIntent
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

class QuickActionHandler(
    private val ragRetriever: RagRetriever,
    private val chunkDao: BookChunkDao,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao,
    private val model: GenerativeModel
) {

    companion object {
        private const val TAG = "QuillAira"
    }

    fun handle(bookId: String, intent: QuickIntent): Flow<AiraResponse> = flow {
        Log.d(TAG, "▶ QUICK ACTION — bookId=$bookId intent=${intent::class.simpleName}")

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            emit(AiraResponse.Error("Book not found"))
            return@flow
        }

        when (intent) {
            is QuickIntent.SummarizeChapter -> {
                val chunks = chunkDao.getChunksForChapter(bookId, intent.chapterIndex)
                if (chunks.isEmpty()) { emit(AiraResponse.Error("No content found for this chapter.")); return@flow }
                val chapterText = chunks.joinToString("\n\n") { it.text }
                streamResponse(bookId, "Summarize this chapter", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Summarize the following chapter text in 3-4 sentences.
                        Focus on the key events, character actions, and any important revelations.
                        Use plain text only. No markdown, no bold, no headers.
                    """.trimIndent(),
                    contextLabel = "CHAPTER TEXT", context = chapterText,
                    question = "Summarize this chapter."
                ))
            }

            is QuickIntent.WhoAreTheCharacters -> {
                val retrieved = ragRetriever.retrieve(bookId, "characters people names persons introduced", spoilerLockEnabled = book.spoilerLockEnabled)
                streamResponse(bookId, "Who are the characters?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Based only on the passages provided, list the key characters the reader
                        has encountered so far. For each, give their name and one brief sentence
                        describing who they are. Use plain text only. No markdown, no bold.
                    """.trimIndent(),
                    contextLabel = "PASSAGES", context = PromptBuilder.buildContextBlock(retrieved.map { it.text }),
                    question = "Who are the characters encountered so far?"
                ))
            }

            is QuickIntent.WhatAreTheThemes -> {
                val retrieved = ragRetriever.retrieve(bookId, "theme meaning symbolism motif central idea", spoilerLockEnabled = book.spoilerLockEnabled)
                streamResponse(bookId, "What are the themes?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Based only on the passages provided, identify the main themes present
                        in what the reader has read so far. Keep the answer to 3-4 sentences.
                        Use plain text only. No markdown, no bold, no headers.
                    """.trimIndent(),
                    contextLabel = "PASSAGES", context = PromptBuilder.buildContextBlock(retrieved.map { it.text }),
                    question = "What are the themes so far?"
                ))
            }

            is QuickIntent.DefineWord -> {
                streamResponse(bookId, "Define: ${intent.word}", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are a precise dictionary. Give a concise definition of the word provided.
                        Include the part of speech and one example sentence if helpful.
                        Keep it to 2-3 sentences maximum. Use plain text only.
                    """.trimIndent(),
                    contextLabel = "BOOK CONTEXT",
                    context = "This word appears in \"${book.title}\" by ${book.author}.",
                    question = "Define the word: ${intent.word}"
                ))
            }

            is QuickIntent.WhatIsThis -> {
                streamResponse(bookId, "What is \"${intent.word}\"?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Explain what this word or term refers to — it may be a place, a dish,
                        an object, a social custom, or a historical concept from the book's era.
                        Keep the explanation to 2-3 sentences. Use plain text only.
                    """.trimIndent(),
                    contextLabel = "BOOK CONTEXT",
                    context = "This term appears in \"${book.title}\" by ${book.author}.",
                    question = "What is \"${intent.word}\"?"
                ))
            }

            is QuickIntent.WhoIsThis -> {
                val chunks = chunkDao.getChunksUpToChapter(bookId, intent.currentChapterIndex)
                if (chunks.isEmpty()) { emit(AiraResponse.Error("No content found up to this chapter.")); return@flow }
                val relevantChunks = chunks.filter { it.text.contains(intent.name, ignoreCase = true) }.take(6).ifEmpty { chunks.take(3) }
                streamResponse(bookId, "Who is \"${intent.name}\"?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Based ONLY on the passages provided, explain who "${intent.name}" is.
                        Describe their role, personality, or relationship to other characters
                        as shown in what the reader has read so far. Do NOT reveal anything
                        beyond what the passages contain. Keep it to 3-4 sentences.
                        Use plain text only. No markdown, no bold.
                    """.trimIndent(),
                    contextLabel = "PASSAGES", context = PromptBuilder.buildContextBlock(relevantChunks.map { it.text }),
                    question = "Who is ${intent.name}?"
                ))
            }

            is QuickIntent.SimplifyThis -> {
                streamResponse(bookId, "Simplify this passage", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Rewrite the following passage in clear, plain modern English.
                        Preserve the meaning exactly — just make it easier to understand.
                        Do not summarize or shorten it significantly. Use plain text only.
                    """.trimIndent(),
                    contextLabel = "PASSAGE", context = intent.text,
                    question = "Rewrite this in plain modern English."
                ))
            }

            is QuickIntent.ExplainThis -> {
                streamResponse(bookId, "Explain this passage", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Explain what the following passage means in context.
                        What is being said or implied? What literary or historical context
                        is relevant? Keep it to 3-4 sentences. Use plain text only.
                    """.trimIndent(),
                    contextLabel = "PASSAGE", context = intent.text,
                    question = "What does this passage mean?"
                ))
            }

            is QuickIntent.WhatIsTheSignificance -> {
                val retrieved = ragRetriever.retrieve(bookId, intent.text, spoilerLockEnabled = book.spoilerLockEnabled)
                streamResponse(bookId, "What's the significance?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Based on the highlighted passage and the surrounding context provided,
                        explain why this moment is significant to the story — its impact on
                        characters, plot, or themes. Keep it to 3-4 sentences. Plain text only.
                    """.trimIndent(),
                    contextLabel = "CONTEXT PASSAGES",
                    context = "HIGHLIGHTED:\n${intent.text}\n\nSURROUNDING CONTEXT:\n${PromptBuilder.buildContextBlock(retrieved.map { it.text })}",
                    question = "Why is this passage significant?"
                ))
            }

            is QuickIntent.WhoIsSpeaking -> {
                val retrieved = ragRetriever.retrieve(bookId, intent.text, spoilerLockEnabled = book.spoilerLockEnabled)
                streamResponse(bookId, "Who's speaking?", PromptBuilder.buildQuickPrompt(
                    systemInstruction = """
                        You are Aira, a reading companion for "${book.title}" by ${book.author}.
                        Based on the highlighted passage and surrounding context, identify
                        who is speaking or narrating. If it is dialogue, say who said it and
                        to whom. If it is narration, say whose perspective it is.
                        Keep it to 2-3 sentences. Use plain text only.
                    """.trimIndent(),
                    contextLabel = "CONTEXT PASSAGES",
                    context = "HIGHLIGHTED:\n${intent.text}\n\nSURROUNDING CONTEXT:\n${PromptBuilder.buildContextBlock(retrieved.map { it.text })}",
                    question = "Who is speaking in this passage?"
                ))
            }
        }
    }

    private suspend fun FlowCollector<AiraResponse>.streamResponse(
        bookId: String,
        userLabel: String,
        prompt: String
    ) {
        try {
            airaMessageDao.insertMessage(
                AiraMessageEntity(bookId = bookId, role = AiraMessageRole.USER, content = userLabel)
            )

            val fullResponseBuilder = StringBuilder()
            model.generateContentStream(prompt).collect { chunk ->
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)
                emit(AiraResponse.Success(fullResponseBuilder.toString().replace("**", "").replace("*", "")))
            }

            val finalAnswer = fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()
            if (finalAnswer.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                airaMessageDao.insertMessage(
                    AiraMessageEntity(bookId = bookId, role = AiraMessageRole.AIRA, content = finalAnswer)
                )
                Log.d(TAG, "  Quick action stream complete. Persisted to DB.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  Quick action Gemini call failed: ${e.message}", e)
            emit(AiraResponse.Error("Error: ${e.message}"))
        }
    }
}