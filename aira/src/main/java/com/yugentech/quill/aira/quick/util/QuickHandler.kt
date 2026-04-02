package com.yugentech.quill.aira.quick.util

import com.google.ai.client.generativeai.GenerativeModel
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class QuickHandler(
    private val model: GenerativeModel,
    private val ragRetriever: RagRetriever,
    private val bookDao: BookDao,
    private val bookChunkDao: BookChunkDao
) {
    fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> = flow {
        Timber.d("========== QUICK HANDLER START ==========")
        Timber.d("Intent: ${quickPrompt::class.simpleName}")
        Timber.d("Book ID: $bookId")

        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            Timber.w("Error: BookEntity not found in DB for ID: $bookId")
            emit(AiraResponse.Error("Book not found."))
            return@flow
        }
        Timber.d("Found Book: '${book.title}' '${book.progressPercent}'  by ${book.author} (Spoiler Lock: ${book.spoilerLockEnabled})")

        val prompt: String = when (quickPrompt) {
            is QuickPrompt.SummarizeChapter -> {
                Timber.d("Action: SummarizeChapter (Index: ${quickPrompt.chapterIndex})")
                val chunks = bookChunkDao.getChunksForChapter(bookId, quickPrompt.chapterIndex)
                Timber.d("Retrieved ${chunks.size} chunks from DB for this chapter.")
                if (chunks.isEmpty()) {
                    Timber.w("No chunks found for chapter index ${quickPrompt.chapterIndex}.")
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                QuickBuilder.summarizeChapter(
                    book.title,
                    book.author,
                    chunks.joinToString("\n\n") { it.text }
                )
            }

            is QuickPrompt.WhoAreTheCharacters -> {
                Timber.d("Action: WhoAreTheCharacters")
                val query = "characters people names persons introduced"
                Timber.d("RAG Query: '$query'")
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = query,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                Timber.d("RAG returned ${retrieved.size} chunks.")
                QuickBuilder.whoAreTheCharacters(
                    book.title,
                    book.author,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhoIsThis -> {
                Timber.d("Action: WhoIsThis")
                Timber.d("Target Name: '${quickPrompt.name}'")

                val query = "${quickPrompt.name} character person description role"
                Timber.d("Sending RAG Query: '$query' (Spoiler Lock forced: true)")

                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = query,
                    spoilerLockEnabled = true // NOTE: Make sure your new chapter logic is passed here if updated!
                )

                Timber.d("RAG Results: Found ${retrieved.size} chunks.")

                if (retrieved.isEmpty()) {
                    Timber.w("RAG returned 0 chunks for '${quickPrompt.name}'. Aborting prompt.")
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }

                // Verbose log to see what exactly RAG thought was relevant
                retrieved.forEachIndexed { index, chunk ->
                    Timber.v("Chunk [$index] (Ch: ${chunk.chapterIndex}, Score: ${chunk.score}): ${chunk.text.take(100)}...")
                }

                val contextBlock = QuickBuilder.buildContextBlock(retrieved.map { it.text })
                Timber.v("Built Context Block (length: ${contextBlock.length} chars)")

                QuickBuilder.whoIsThis(
                    book.title,
                    book.author,
                    quickPrompt.name,
                    contextBlock
                )
            }

            is QuickPrompt.WhatAreTheThemes -> {
                Timber.d("Action: WhatAreTheThemes")
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "theme meaning symbolism motif central idea",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                Timber.d("RAG returned ${retrieved.size} chunks.")
                QuickBuilder.whatAreTheThemes(
                    book.title,
                    book.author,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.DefineWord -> {
                Timber.d("Action: DefineWord ('${quickPrompt.word}')")
                QuickBuilder.defineWord(book.title, book.author, quickPrompt.word)
            }

            is QuickPrompt.WhatIsThis -> {
                Timber.d("Action: WhatIsThis ('${quickPrompt.word}')")
                QuickBuilder.whatIsThis(book.title, book.author, quickPrompt.word)
            }

            is QuickPrompt.SimplifyThis -> {
                Timber.d("Action: SimplifyThis (text length: ${quickPrompt.text.length})")
                QuickBuilder.simplifyThis(book.title, book.author, quickPrompt.text)
            }

            is QuickPrompt.ExplainThis -> {
                Timber.d("Action: ExplainThis (text length: ${quickPrompt.text.length})")
                QuickBuilder.explainThis(book.title, book.author, quickPrompt.text)
            }

            is QuickPrompt.WhatIsTheSignificance -> {
                Timber.d("Action: WhatIsTheSignificance")
                Timber.d("Query Text: '${quickPrompt.text}'")
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                Timber.d("RAG returned ${retrieved.size} chunks.")
                QuickBuilder.whatIsTheSignificance(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhoIsSpeaking -> {
                Timber.d("Action: WhoIsSpeaking")
                Timber.d("Query Text: '${quickPrompt.text}'")
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                Timber.d("RAG returned ${retrieved.size} chunks.")
                QuickBuilder.whoIsSpeaking(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }
        }

        Timber.v("Final LLM Prompt built. Length: ${prompt.length} chars.")
        streamResponse(prompt, quickPrompt)
    }

    private suspend fun FlowCollector<AiraResponse>.streamResponse(
        prompt: String,
        intent: QuickPrompt
    ) {
        Timber.d("Starting LLM Generation Stream...")
        try {
            val fullResponseBuilder = StringBuilder()
            var chunkCount = 0

            model.generateContentStream(prompt).collect { chunk ->
                chunkCount++
                val textChunk = chunk.text ?: ""
                fullResponseBuilder.append(textChunk)
                emit(
                    AiraResponse.Success(
                        fullResponseBuilder.toString().replace("**", "").replace("*", "")
                    )
                )
            }

            Timber.d("Stream finished. Received $chunkCount total chunks.")

            val finalAnswer = fullResponseBuilder.toString().replace("**", "").replace("*", "").trim()
            if (finalAnswer.isBlank()) {
                Timber.w("Warning: LLM returned an empty or blank response.")
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                Timber.d("✓ Response successfully emitted. Final answer length: ${finalAnswer.length}")
            }

            Timber.d("========== QUICK HANDLER DONE ==========")

        } catch (e: Exception) {
            Timber.e(e, "Gemini call failed for intent: ${intent::class.simpleName}")
            emit(AiraResponse.Error("Something went wrong. Please try again."))
        }
    }
}