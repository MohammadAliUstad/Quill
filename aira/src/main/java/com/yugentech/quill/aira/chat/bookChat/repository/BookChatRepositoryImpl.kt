package com.yugentech.quill.aira.chat.bookChat.repository

import com.yugentech.quill.aira.chat.bookChat.payload.BookChatPayload
import com.yugentech.quill.aira.chat.bookChat.service.BookChatService
import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.util.AiraBuilder
import com.yugentech.quill.aira.util.ChatUtils
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import timber.log.Timber

class BookChatRepositoryImpl(
    private val chatService: BookChatService,
    private val ragRetriever: RagRetriever
) : BookChatRepository {

    override fun handle(
        question: String,
        route: Intent.BookRelated,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        Timber.d("BookChatRepo: Handling book-related query. Route=$route")
        val chunks = ragRetriever.retrieveWithExpansion(
            bookId = book.id,
            queries = route.queryVariations,
            entities = route.entities,
            boostedKeywords = (route.entities + route.keywords).distinct(),
            topPassages = route.intent.topPassages,
            candidatesPerQuery = route.intent.candidatesPerQuery,
            spoilerLockEnabled = book.spoilerLockEnabled
        )
        Timber.d("BookChatRepo: Retrieved ${chunks.size} chunks from RAG")

        val contextBlock = AiraBuilder.buildContextBlock(chunks)

        val payload =
            BookChatPayload(
                query = question,
                context = contextBlock,
                bookTitle = book.title,
                bookAuthor = book.author,
                history = ChatUtils.formatHistory(history)
            )
        Timber.d("BookChatRepo: Sending payload to service: $payload")

        try {
            val rawResponse = chatService.bookChat(payload)
            Timber.d("BookChatRepo: Raw response from service: '$rawResponse'")

            try {
                val cleaned = rawResponse
                    .replace("```json", "", ignoreCase = true)
                    .replace("```", "")
                    .trim()

                val startIndex = cleaned.indexOf('{')
                val endIndex = cleaned.lastIndexOf('}')

                if (startIndex == -1 || endIndex == -1) {
                    Timber.w("BookChatRepo: No JSON found in response, returning raw text")
                    emit(AiraResponse.Success(text = rawResponse))
                    return@flow
                }

                val json = JSONObject(cleaned.substring(startIndex, endIndex + 1))
                val answer = json.getString("answer")
                Timber.d("BookChatRepo: Extracted answer: '$answer'")

                emit(AiraResponse.Success(text = answer))
            } catch (e: Exception) {
                Timber.e(e, "BookChatRepo: JSON parsing failed")
                emit(AiraResponse.Success(text = rawResponse))
            }
        } catch (e: Exception) {
            Timber.e(e, "BookChatRepo: Service call failed")
            val errorMsg = when {
                e.message?.contains("resource-exhausted") == true ->
                    "You've reached your free limit. Upgrade to Quill Pro."
                else -> "Something went wrong. Please try again."
            }
            emit(AiraResponse.Error(errorMsg))
        }
    }
}
