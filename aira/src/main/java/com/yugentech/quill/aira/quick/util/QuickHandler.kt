package com.yugentech.quill.aira.quick.util

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class QuickHandler(
    private val bookDao: BookDao,
    private val bookChunkDao: BookChunkDao,
    private val functions: FirebaseFunctions,
    private val ragRetriever: RagRetriever
) {
    fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> = flow {
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
                QuickBuilder.summarizeChapter(
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
                QuickBuilder.whoAreTheCharacters(
                    book.title,
                    book.author,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
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
                QuickBuilder.whoIsThis(
                    book.title,
                    book.author,
                    quickPrompt.name,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhatAreTheThemes -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "theme meaning symbolism motif central idea",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickBuilder.whatAreTheThemes(
                    book.title,
                    book.author,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.DefineWord -> QuickBuilder.defineWord(book.title, book.author, quickPrompt.word)

            is QuickPrompt.WhatIsThis -> QuickBuilder.whatIsThis(book.title, book.author, quickPrompt.word)

            is QuickPrompt.SimplifyThis -> QuickBuilder.simplifyThis(book.title, book.author, quickPrompt.text)

            is QuickPrompt.ExplainThis -> QuickBuilder.explainThis(book.title, book.author, quickPrompt.text)

            is QuickPrompt.WhatIsTheSignificance -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickBuilder.whatIsTheSignificance(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhoIsSpeaking -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickBuilder.whoIsSpeaking(
                    book.title,
                    book.author,
                    quickPrompt.text,
                    QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }
        }

        try {
            val payload = hashMapOf(
                "prompt" to prompt,
                "systemPrompt" to "",
                "history" to emptyList<Any>()
            )

            val result = functions
                .getHttpsCallable("airaChat")
                .call(payload)
                .await()

            val responseText = (result.getData() as? Map<*, *>)?.get("response") as? String

            if (responseText.isNullOrBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                emit(AiraResponse.Success(responseText.trim()))
            }

        } catch (e: Exception) {
            Timber.e(e, "Quick action function call failed")
            val errorMsg = when {
                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."
                else -> "Something went wrong. Please try again."
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }
}