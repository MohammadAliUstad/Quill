package com.yugentech.quill.aira.chat.quickChat.repository

import com.yugentech.quill.aira.chat.quickChat.model.QuickChatPayload
import com.yugentech.quill.aira.chat.quickChat.model.QuickChatType
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.aira.chat.quickChat.service.QuickChatService
import com.yugentech.quill.aira.chat.quickChat.util.QuickBuilder
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class QuickChatRepositoryImpl(
    private val bookDao: BookDao,
    private val bookChunkDao: BookChunkDao,
    private val actionService: QuickChatService,
    private val ragRetriever: RagRetriever
) : QuickChatRepository {

    override suspend fun ask(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> = flow {
        Timber.d("QuickChatRepo: Handling quick prompt: $quickPrompt")
        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            Timber.e("QuickChatRepo: Book not found for id=$bookId")
            emit(AiraResponse.Error("Book not found."))
            return@flow
        }

        val payload: QuickChatPayload = when (quickPrompt) {
            is QuickPrompt.SummarizeChapter -> {
                val chunks = bookChunkDao.getChunksForChapter(bookId, quickPrompt.chapterIndex)
                if (chunks.isEmpty()) {
                    Timber.w("QuickChatRepo: No chunks found for chapter ${quickPrompt.chapterIndex}")
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                QuickChatPayload(
                    actionType = QuickChatType.SUMMARIZE_CHAPTER,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = chunks.joinToString("\n\n") { it.text }
                )
            }

            is QuickPrompt.WhoAreTheCharacters -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "characters people names persons introduced",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHO_ARE_CHARACTERS,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.WhoIsThis -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "${quickPrompt.name} character person description role",
                    spoilerLockEnabled = true
                )
                if (retrieved.isEmpty()) {
                    Timber.w("QuickChatRepo: No context found for character '${quickPrompt.name}'")
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }
                QuickChatPayload(
                    actionType = QuickChatType.WHO_IS_THIS,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved.map { it.text }),
                    query = quickPrompt.name
                )
            }

            is QuickPrompt.WhatAreTheThemes -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "theme meaning symbolism motif central idea",
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHAT_ARE_THEMES,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved.map { it.text })
                )
            }

            is QuickPrompt.DefineWord -> QuickChatPayload(
                actionType = QuickChatType.DEFINE_WORD,
                bookTitle = book.title,
                bookAuthor = book.author,
                context = "",
                query = quickPrompt.word
            )

            is QuickPrompt.WhatIsThis -> QuickChatPayload(
                actionType = QuickChatType.WHAT_IS_THIS,
                bookTitle = book.title,
                bookAuthor = book.author,
                context = "",
                query = quickPrompt.word
            )

            is QuickPrompt.SimplifyThis -> QuickChatPayload(
                actionType = QuickChatType.SIMPLIFY_THIS,
                bookTitle = book.title,
                bookAuthor = book.author,
                context = quickPrompt.text
            )

            is QuickPrompt.ExplainThis -> QuickChatPayload(
                actionType = QuickChatType.EXPLAIN_THIS,
                bookTitle = book.title,
                bookAuthor = book.author,
                context = quickPrompt.text
            )

            is QuickPrompt.WhatIsTheSignificance -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHAT_SIGNIFICANCE,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = "HIGHLIGHTED:\n${quickPrompt.text}\n\nSURROUNDING CONTEXT:\n${QuickBuilder.buildContextBlock(retrieved.map { it.text })}"
                )
            }

            is QuickPrompt.WhoIsSpeaking -> {
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    spoilerLockEnabled = book.spoilerLockEnabled
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHO_IS_SPEAKING,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = "HIGHLIGHTED:\n${quickPrompt.text}\n\nSURROUNDING CONTEXT:\n${QuickBuilder.buildContextBlock(retrieved.map { it.text })}"
                )
            }
        }
        Timber.d("QuickChatRepo: Sending payload to service: $payload")

        try {
            val responseText = actionService.getQuickChatResponse(payload)
            Timber.d("QuickChatRepo: Raw response from service: '$responseText'")

            if (responseText.isBlank()) {
                Timber.w("QuickChatRepo: Service returned blank response")
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                emit(AiraResponse.Success(responseText.trim()))
            }

        } catch (e: Exception) {
            Timber.e(e, "QuickChatRepo: Service call failed")
            val errorMsg = when {
                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."
                else -> "Something went wrong. Please try again."
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }
}
