package com.yugentech.quill.aira.aira.chat.book

import com.yugentech.quill.aira.aira.chat.model.ChatPayload
import com.yugentech.quill.aira.aira.chat.model.ChatRequestType
import com.yugentech.quill.aira.aira.chat.service.AiraChatService
import com.yugentech.quill.aira.aira.chat.util.ChatUtils
import com.yugentech.quill.aira.aira.intentDetection.model.AiraIntent
import com.yugentech.quill.aira.aira.util.AiraBuilder
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class BookChatHandler(
    private val chatService: AiraChatService,
    private val ragRetriever: RagRetriever
) {
    fun handle(
        question: String,
        route: AiraIntent.BookRelated,
        history: List<AiraMessageEntity>,
        book: BookEntity
    ): Flow<AiraResponse> = flow {
        val chunks = ragRetriever.retrieveWithExpansion(
            bookId = book.id,
            queries = route.queryVariations,
            entities = route.entities,
            boostedKeywords = (route.entities + route.keywords).distinct(),
            topPassages = route.intent.topPassages,
            candidatesPerQuery = route.intent.candidatesPerQuery,
            spoilerLockEnabled = book.spoilerLockEnabled
        )

        val contextBlock = AiraBuilder.buildContextBlock(chunks)
        
        val payload = ChatPayload(
            prompt = question,
            context = contextBlock,
            requestType = ChatRequestType.BOOK,
            bookTitle = book.title,
            bookAuthor = book.author,
            history = ChatUtils.formatHistory(history)
        )

        val rawResponse = chatService.getChatResponse(payload)
        
        try {
            val cleaned = rawResponse
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()
                
            val startIndex = cleaned.indexOf('{')
            val endIndex = cleaned.lastIndexOf('}')
            
            if (startIndex == -1 || endIndex == -1) {
                emit(AiraResponse.Success(text = rawResponse, sources = chunks))
                return@flow
            }
            
            val json = JSONObject(cleaned.substring(startIndex, endIndex + 1))
            val answer = json.getString("answer")
            val usedIdsArray = json.optJSONArray("used_ids")
            
            val usedIds = mutableListOf<Int>()
            if (usedIdsArray != null) {
                for (i in 0 until usedIdsArray.length()) {
                    usedIds.add(usedIdsArray.getInt(i))
                }
            }
            
            val accurateSources = if (AiraBuilder.isDeadEnd(answer)) {
                emptyList()
            } else {
                chunks.filterIndexed { index, _ -> index in usedIds }
            }
            
            emit(AiraResponse.Success(text = answer, sources = accurateSources))
        } catch (e: Exception) {
            val fallbackSources = if (AiraBuilder.isDeadEnd(rawResponse)) emptyList() else chunks
            emit(AiraResponse.Success(text = rawResponse, sources = fallbackSources))
        }
    }
}
