package com.yugentech.quill.reader.quickPrompt.util

import com.google.ai.client.generativeai.GenerativeModel
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.reader.quickPrompt.state.QuickPrompt
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class QuickPromptHandler(
    private val model: GenerativeModel,
    private val ragRetriever: RagRetriever,
    private val bookDao: BookDao,
    private val bookChunkDao: BookChunkDao
) {
    fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> = flow {
        Timber.d("Quick Prompt — ${quickPrompt::class.simpleName}")

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            emit(AiraResponse.Error("Book not found."))
            return@flow
        }

        val prompt: String = when (quickPrompt) {
            is QuickPrompt.SummarizeChapter -> {
                val chunks = bookChunkDao.getChunksForChapter(bookId, quickPrompt.chapterIndex)
                if (chunks.isEmpty()) {
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                QuickPromptBuilder.summarizeChapter(
                    book.title,
                    book.author,
                    chunks.joinToString("\n\n") { it.text }
                )
            }

            is QuickPrompt.WhoAreTheCharacters -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "characters people names persons introduced",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickPromptBuilder.whoAreTheCharacters(
                    book.title,
                    book.author,
                    QuickPromptBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhatAreTheThemes -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "theme meaning symbolism motif central idea",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickPromptBuilder.whatAreTheThemes(
                    book.title,
                    book.author,
                    QuickPromptBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.DefineWord -> {
                QuickPromptBuilder.defineWord(book.title, book.author, quickPrompt.word)
            }

            is QuickPrompt.WhatIsThis -> {
                QuickPromptBuilder.whatIsThis(book.title, book.author, quickPrompt.word)
            }

            is QuickPrompt.WhoIsThis -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "${quickPrompt.name} character person description role",
                    spoilerLockEnabled = true
                )
                if (retrieved.isEmpty()) {
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }
                QuickPromptBuilder.whoIsThis(
                    book.title,
                    book.author,
                    quickPrompt.name,
                    QuickPromptBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.SimplifyThis -> {
                QuickPromptBuilder.simplifyThis(book.title, book.author, quickPrompt.text)
            }

            is QuickPrompt.ExplainThis -> {
                QuickPromptBuilder.explainThis(book.title, book.author, quickPrompt.text)
            }

            is QuickPrompt.WhatIsTheSignificance -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickPromptBuilder.whatIsTheSignificance(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickPromptBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhoIsSpeaking -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickPromptBuilder.whoIsSpeaking(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickPromptBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }
        }

        streamResponse(prompt, quickPrompt)
    }

    private suspend fun FlowCollector<AiraResponse>.streamResponse(prompt: String, intent: QuickPrompt) {
        try {
            val fullResponseBuilder = StringBuilder()
            model.generateContentStream(prompt).collect { chunk ->
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)
                emit(
                    AiraResponse.Success(
                        fullResponseBuilder.toString().replace("**", "").replace("*", "")
                    )
                )
            }

            val finalAnswer = fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()
            if (finalAnswer.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            }

            Timber.d("✓ Stream complete for ${intent::class.simpleName}")

        } catch (e: Exception) {
            Timber.e(e, "Gemini call failed for intent: ${intent::class.simpleName}")
            emit(AiraResponse.Error("Something went wrong. Please try again."))
        }
    }
}