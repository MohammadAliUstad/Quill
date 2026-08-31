package com.yugentech.quill.aira.chat.quickChat.repository

import android.content.Context
import android.util.Base64
import com.google.firebase.functions.FirebaseFunctionsException
import com.yugentech.quill.aira.chat.quickChat.model.QuickChatPayload
import com.yugentech.quill.aira.chat.quickChat.model.QuickChatType
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.aira.chat.quickChat.service.QuickChatService
import com.yugentech.quill.aira.chat.quickChat.service.VisualizeService
import com.yugentech.quill.aira.chat.quickChat.util.QuickBuilder
import com.yugentech.quill.aira.rag.EpubTextExtractor
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.service.AiraChatService
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.VisualDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import com.yugentech.quill.database.entity.VisualEntity
import com.yugentech.quill.database.model.RetrievedChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import java.util.UUID

class QuickChatRepositoryImpl(
    private val context: Context,
    private val bookDao: BookDao,
    private val visualDao: VisualDao,
    private val airaMessageDao: AiraMessageDao,
    private val epubTextExtractor: EpubTextExtractor,
    private val actionService: QuickChatService,
    private val visualizeService: VisualizeService,
    private val airaChatService: AiraChatService,
    private val ragRetriever: RagRetriever
) : QuickChatRepository {

    override suspend fun ask(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> = flow {
        val book = bookDao.getBookEntity(bookId)
        if (book == null) {
            emit(AiraResponse.Error("Book not found."))
            return@flow
        }

        val payload: QuickChatPayload = when (quickPrompt) {
            is QuickPrompt.SummarizeChapter -> {
                val localFilePath = book.localFilePath
                val chapter = localFilePath?.let {
                    epubTextExtractor.extractChapter(it, quickPrompt.chapterIndex)
                }
                if (chapter == null) {
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                QuickChatPayload(
                    actionType = QuickChatType.SUMMARIZE_CHAPTER,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = chapter.text
                )
            }

            is QuickPrompt.WhoAreTheCharacters -> {
                // The default topPassages (3 anchors, each expanded to only its immediate
                // neighbors) leaves barely a handful of small chunks to cover every character in
                // the book -- enough to name people, not enough to say anything real about them.
                // A much larger budget gives each character a real chance at more than one
                // mention to draw a description from.
                val retrieved = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "characters people names persons introduced",
                    topPassages = 40,
                    spoilerLockEnabled = true
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHO_ARE_CHARACTERS,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved)
                )
            }

            is QuickPrompt.RecallCharacter -> {
                val retrieved = ragRetriever.retrieveForRecallCharacter(
                    bookId = bookId,
                    characterName = quickPrompt.name,
                    spoilerLockEnabled = true
                )
                if (retrieved.isEmpty()) {
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }
                logRetrievedChunks("RecallCharacter", quickPrompt.name, retrieved)
                QuickChatPayload(
                    actionType = QuickChatType.RECALL_CHARACTER,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved),
                    query = quickPrompt.name
                )
            }

            is QuickPrompt.RecentRole -> {
                val retrieved = ragRetriever.retrieveForRecentRole(
                    bookId = bookId,
                    characterName = quickPrompt.name,
                    spoilerLockEnabled = true
                )
                if (retrieved.isEmpty()) {
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }
                logRetrievedChunks("RecentRole", quickPrompt.name, retrieved)
                QuickChatPayload(
                    actionType = QuickChatType.RECENT_ROLE,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved),
                    query = quickPrompt.name
                )
            }

            is QuickPrompt.JourneySoFar -> {
                val retrieved = ragRetriever.retrieveForJourney(
                    bookId = bookId,
                    characterName = quickPrompt.name,
                    spoilerLockEnabled = true
                )
                if (retrieved.isEmpty()) {
                    emit(AiraResponse.Error("I haven't encountered \"${quickPrompt.name}\" in what you've read so far."))
                    return@flow
                }
                logRetrievedChunks("JourneySoFar", quickPrompt.name, retrieved)
                QuickChatPayload(
                    actionType = QuickChatType.JOURNEY_SO_FAR,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = QuickBuilder.buildContextBlock(retrieved),
                    query = quickPrompt.name
                )
            }

            is QuickPrompt.WhatAreTheThemes -> {
                val localFilePath = book.localFilePath
                val chapter = localFilePath?.let {
                    epubTextExtractor.extractChapter(it, quickPrompt.chapterIndex)
                }
                if (chapter == null) {
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                // The current chapter stays the anchor -- these retrieved passages from
                // elsewhere in the book are purely supplementary, giving the model enough to
                // notice a theme carrying through or recurring rather than treating the
                // chapter in isolation.
                val supplementary = ragRetriever.retrieve(
                    bookId = bookId,
                    query = "theme meaning symbolism motif central idea",
                    spoilerLockEnabled = true
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHAT_ARE_THEMES,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = "CURRENT CHAPTER:\n${chapter.text}\n\n" +
                        "ADDITIONAL CONTEXT FROM ELSEWHERE IN THE BOOK:\n" +
                        QuickBuilder.buildContextBlock(supplementary)
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

            is QuickPrompt.ExplainThis -> QuickChatPayload(
                actionType = QuickChatType.EXPLAIN_THIS,
                bookTitle = book.title,
                bookAuthor = book.author,
                context = quickPrompt.text
            )

            is QuickPrompt.WhatIsTheSignificance -> {
                // Significance usually hinges on the immediate scene around the highlight --
                // what led up to it, what it's reacting to -- which a pure semantic search
                // keyed on the highlight itself isn't guaranteed to surface (nearby dialogue can
                // drift topic fast even though it's exactly what matters here). The full current
                // chapter covers that; the retrieved passages are purely supplementary, for
                // catching an echo or a connection to something earlier in the book.
                val localFilePath = book.localFilePath
                val chapter = localFilePath?.let {
                    epubTextExtractor.extractChapter(it, quickPrompt.chapterIndex)
                }
                if (chapter == null) {
                    emit(AiraResponse.Error("No content found for this chapter."))
                    return@flow
                }
                val supplementary = ragRetriever.retrieve(
                    bookId = bookId,
                    query = quickPrompt.text,
                    topPassages = 10,
                    spoilerLockEnabled = true
                )
                QuickChatPayload(
                    actionType = QuickChatType.WHAT_SIGNIFICANCE,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    context = "CURRENT CHAPTER:\n${chapter.text}\n\n" +
                        "HIGHLIGHTED PASSAGE WITHIN IT:\n${quickPrompt.text}\n\n" +
                        "ADDITIONAL CONTEXT FROM ELSEWHERE IN THE BOOK:\n" +
                        QuickBuilder.buildContextBlock(supplementary)
                )
            }

            // Isolated flow, like the old RecallCharacterSimple test path -- its own service
            // call, its own storage, and its own emitted response type. It never produces a
            // QuickChatPayload, so it ends with return@flow.
            is QuickPrompt.VisualizeThis -> {
                try {
                    val result = visualizeService.visualize(
                        bookTitle = book.title,
                        bookAuthor = book.author,
                        passage = quickPrompt.text
                    )

                    val imageBytes = Base64.decode(result.imageBase64, Base64.DEFAULT)
                    val visualsDir = File(context.filesDir, "visuals").also { it.mkdirs() }
                    val id = UUID.randomUUID().toString()
                    val extension = if (result.mimeType.contains("png")) "png" else "jpg"
                    val imageFile = File(visualsDir, "$id.$extension")
                    imageFile.writeBytes(imageBytes)

                    val chapterTitle = book.localFilePath
                        ?.let { epubTextExtractor.extractChapter(it, quickPrompt.chapterIndex) }
                        ?.chapterTitle
                        ?: "Chapter ${quickPrompt.chapterIndex + 1}"

                    visualDao.insertVisual(
                        VisualEntity(
                            id = id,
                            bookId = bookId,
                            chapterIndex = quickPrompt.chapterIndex,
                            chapterTitle = chapterTitle,
                            sourceText = quickPrompt.text,
                            imagePath = imageFile.absolutePath,
                            locatorJson = quickPrompt.locatorJson
                        )
                    )

                    emit(AiraResponse.ImageSuccess(imageFile.absolutePath))
                } catch (e: Exception) {
                    Timber.e(e, "VisualizeThis failed")
                    val errorMsg = when {
                        e.message?.contains("resource-exhausted") == true ->
                            "You've reached your free limit. Upgrade to Quill Pro."
                        // A clean model decline (nothing visual in the passage) rather than a
                        // technical failure -- the function already puts a friendly, specific
                        // message here, so just pass it through instead of the generic fallback.
                        (e as? FirebaseFunctionsException)?.code ==
                            FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                            e.message ?: "This passage doesn't have enough visual detail to generate an image."
                        else -> "Something went wrong generating that image. Please try again."
                    }
                    emit(AiraResponse.Error(errorMsg))
                }

                return@flow
            }

            // Isolated flow, like VisualizeThis -- delegates straight into the same
            // intent-detection + book/general chat pipeline AiraChatScreen uses, with the
            // highlighted text threaded through as extra anchor context. That's what gives it
            // entity extraction, multi-query retrieval, and conversation history, instead of
            // the single thin generic search this used to run on its own.
            is QuickPrompt.CustomQuestion -> {
                emitAll(
                    airaChatService.ask(
                        bookId = bookId,
                        query = quickPrompt.query,
                        selectedText = quickPrompt.selectedText
                    )
                )
                return@flow
            }
        }

        try {
            val responseText = actionService.getQuickChatResponse(payload)
            Timber.d("QuickChat[${payload.actionType}] query=\"${payload.query}\" response: $responseText")

            if (responseText.isBlank()) {
                emit(AiraResponse.Error("Aira didn't have a response."))
            } else {
                // Quick actions (chips) don't come from the user typing a real question, so they
                // get recorded under a premade stand-in statement -- this is what makes them show
                // up in the full chat history/transcript alongside typed messages, instead of
                // vanishing once the peek bar closes.
                airaMessageDao.insertMessage(
                    AiraMessageEntity(
                        bookId = bookId,
                        role = AiraMessageRole.USER,
                        content = quickPrompt.toHistoryStatement()
                    )
                )
                airaMessageDao.insertMessage(
                    AiraMessageEntity(
                        bookId = bookId,
                        role = AiraMessageRole.AIRA,
                        content = responseText.trim()
                    )
                )
                emit(AiraResponse.Success(responseText.trim()))
            }

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."
                else -> "Something went wrong. Please try again."
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }

    // Shows exactly which chapters/chunks are going into the LLM call for the
    // character-retrieval quick actions, without dumping the full chunk text.
    private fun logRetrievedChunks(label: String, name: String, chunks: List<RetrievedChunk>) {
        Timber.d(
            "$label chunks for \"$name\" (${chunks.size}):\n" +
                chunks.joinToString("\n") { chunk ->
                    "[Ch ${chunk.chapterIndex} chunk ${chunk.chunkIndex}] ${chunk.text.take(80)}"
                }
        )
    }

    // A chip tap never produces a real typed question, so one is stood in for it here purely
    // for the chat history transcript -- CustomQuestion is the one exception, since it already
    // carries the user's own typed text.
    private fun QuickPrompt.toHistoryStatement(): String = when (this) {
        is QuickPrompt.WhoAreTheCharacters -> "Who are the characters in this book?"
        is QuickPrompt.WhatAreTheThemes -> "What are the themes of this book?"
        is QuickPrompt.RecallCharacter -> "Who is $name?"
        is QuickPrompt.RecentRole -> "What has $name been doing recently?"
        is QuickPrompt.JourneySoFar -> "What has $name's journey been so far?"
        is QuickPrompt.WhatIsTheSignificance -> "What's the significance of \"$text\"?"
        is QuickPrompt.SummarizeChapter -> "Can you summarize this chapter?"
        is QuickPrompt.DefineWord -> "What does $word mean?"
        is QuickPrompt.WhatIsThis -> "What is $word?"
        is QuickPrompt.ExplainThis -> "Can you explain \"$text\"?"
        is QuickPrompt.VisualizeThis -> "Can you visualize \"$text\"?"
        is QuickPrompt.CustomQuestion -> query
    }
}
