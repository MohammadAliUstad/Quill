package com.yugentech.quill.aira.chat.bookChat.repository

import com.yugentech.quill.aira.chat.bookChat.payload.BookChatPayload
import com.yugentech.quill.aira.chat.bookChat.service.BookChatService
import com.yugentech.quill.aira.intent.model.Intent
import com.yugentech.quill.aira.intent.model.QueryIntent
import com.yugentech.quill.aira.rag.EpubTextExtractor
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.aira.util.AiraBuilder
import com.yugentech.quill.aira.util.ChatUtils
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.RetrievedChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import timber.log.Timber

class BookChatRepositoryImpl(
    private val chatService: BookChatService,
    private val ragRetriever: RagRetriever,
    private val epubTextExtractor: EpubTextExtractor
) : BookChatRepository {

    override fun handle(
        question: String,
        route: Intent.BookRelated,
        history: List<AiraMessageEntity>,
        book: BookEntity,
        selectedText: String?,
        userName: String?
    ): Flow<AiraResponse> = flow {
        val contextBlock = buildContextBlock(route, book)

        val payload =
            BookChatPayload(
                query = question,
                context = contextBlock,
                bookTitle = book.title,
                bookAuthor = book.author,
                history = ChatUtils.formatHistory(history),
                queryIntent = route.intent.name.lowercase(),
                selectedText = selectedText,
                userName = userName
            )

        try {
            val rawResponse = chatService.bookChat(payload)
            Timber.d("BookChat[${route.intent}] question=\"$question\" response: $rawResponse")

            try {
                val cleaned = rawResponse
                    .replace("```json", "", ignoreCase = true)
                    .replace("```", "")
                    .trim()

                val startIndex = cleaned.indexOf('{')
                val endIndex = cleaned.lastIndexOf('}')

                if (startIndex == -1 || endIndex == -1) {
                    emit(AiraResponse.Success(text = rawResponse))
                    return@flow
                }

                val json = JSONObject(cleaned.substring(startIndex, endIndex + 1))
                val answer = json.getString("answer")

                emit(AiraResponse.Success(text = answer))
            } catch (e: Exception) {
                emit(AiraResponse.Success(text = rawResponse))
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

    // Routes each classified intent to whichever retrieval already does the best job for that
    // shape of question -- the same specialized functions the quick-action chips use -- falling
    // back to generic multi-query retrieval whenever a specialized path can't be used (e.g. the
    // classifier didn't confidently extract a character name).
    private suspend fun buildContextBlock(route: Intent.BookRelated, book: BookEntity): String {
        return when (route.intent) {
            QueryIntent.CHAPTER_SUMMARY -> {
                // Always the reader's actual current chapter -- never a chapter number parsed
                // out of the question, which risks either resolving to the wrong chapter or
                // spoiling one the reader hasn't reached yet.
                extractCurrentChapterText(book)?.also {
                    Timber.d("BookChat[CHAPTER_SUMMARY] chapterIndex=${book.lastChapterIndex} chars=${it.length}")
                } ?: run {
                    Timber.d("BookChat[CHAPTER_SUMMARY] no chapter text -- falling back to generic retrieval")
                    buildGenericContextBlock(route, book)
                }
            }

            QueryIntent.CHARACTER_PROFILE -> {
                val name = route.characterName
                if (name.isNullOrBlank()) {
                    Timber.d("BookChat[CHARACTER_PROFILE] no characterName -- falling back to generic retrieval")
                    buildGenericContextBlock(route, book)
                } else {
                    val chunks = ragRetriever.retrieveForRecallCharacter(
                        bookId = book.id,
                        characterName = name,
                        spoilerLockEnabled = book.spoilerLockEnabled
                    )
                    logChunks("CHARACTER_PROFILE:$name", chunks)
                    chunks.ifEmpty { null }?.let { AiraBuilder.buildContextBlock(it) }
                        ?: buildGenericContextBlock(route, book)
                }
            }

            QueryIntent.CHARACTER_RECENT -> {
                val name = route.characterName
                if (name.isNullOrBlank()) {
                    Timber.d("BookChat[CHARACTER_RECENT] no characterName -- falling back to generic retrieval")
                    buildGenericContextBlock(route, book)
                } else {
                    val chunks = ragRetriever.retrieveForRecentRole(
                        bookId = book.id,
                        characterName = name,
                        spoilerLockEnabled = book.spoilerLockEnabled
                    )
                    logChunks("CHARACTER_RECENT:$name", chunks)
                    chunks.ifEmpty { null }?.let { AiraBuilder.buildContextBlock(it) }
                        ?: buildGenericContextBlock(route, book)
                }
            }

            QueryIntent.CHARACTER_ARC -> {
                val name = route.characterName
                if (name.isNullOrBlank()) {
                    Timber.d("BookChat[CHARACTER_ARC] no characterName -- falling back to generic retrieval")
                    buildGenericContextBlock(route, book)
                } else {
                    val chunks = ragRetriever.retrieveForJourney(
                        bookId = book.id,
                        characterName = name,
                        spoilerLockEnabled = book.spoilerLockEnabled
                    )
                    logChunks("CHARACTER_ARC:$name", chunks)
                    chunks.ifEmpty { null }?.let { AiraBuilder.buildContextBlock(it) }
                        ?: buildGenericContextBlock(route, book)
                }
            }

            QueryIntent.THEME_ANALYSIS, QueryIntent.SIGNIFICANCE -> {
                if (route.isChapterScoped) {
                    val chapterText = extractCurrentChapterText(book)
                    if (chapterText == null) {
                        Timber.d("BookChat[${route.intent}] chapter-scoped but no chapter text -- falling back to generic retrieval")
                        buildGenericContextBlock(route, book)
                    } else {
                        val supplementary = genericChunks(route, book, topPassagesOverride = 8)
                        Timber.d("BookChat[${route.intent}] chapter-scoped: chapterIndex=${book.lastChapterIndex} chars=${chapterText.length}")
                        logChunks("${route.intent}:supplementary", supplementary)
                        "CURRENT CHAPTER:\n$chapterText\n\n" +
                            "ADDITIONAL CONTEXT FROM ELSEWHERE IN THE BOOK:\n" +
                            AiraBuilder.buildContextBlock(supplementary)
                    }
                } else {
                    // Whole-book scope genuinely benefits from a wider net than the default --
                    // there's no single chapter anchoring the answer here.
                    val chunks = genericChunks(route, book, topPassagesOverride = 15)
                    logChunks("${route.intent}:wholeBook", chunks)
                    AiraBuilder.buildContextBlock(chunks)
                }
            }

            else -> buildGenericContextBlock(route, book)
        }
    }

    private suspend fun buildGenericContextBlock(route: Intent.BookRelated, book: BookEntity): String {
        val chunks = genericChunks(route, book)
        logChunks("${route.intent}:generic", chunks)
        return AiraBuilder.buildContextBlock(chunks)
    }

    // Mirrors QuickChatRepositoryImpl's logRetrievedChunks -- shows exactly which
    // chapters/chunks are going into the LLM call, without dumping the full chunk text.
    private fun logChunks(label: String, chunks: List<RetrievedChunk>) {
        Timber.d(
            "BookChat[$label] retrieved ${chunks.size} chunks:\n" +
                chunks.joinToString("\n") { chunk ->
                    "[Ch ${chunk.chapterIndex} chunk ${chunk.chunkIndex}] ${chunk.text.take(80)}"
                }
        )
    }

    private suspend fun genericChunks(
        route: Intent.BookRelated,
        book: BookEntity,
        topPassagesOverride: Int? = null
    ): List<RetrievedChunk> = ragRetriever.retrieveWithExpansion(
        bookId = book.id,
        queries = route.queryVariations,
        entities = route.entities,
        boostedKeywords = (route.entities + route.keywords).distinct(),
        topPassages = topPassagesOverride ?: route.intent.topPassages,
        candidatesPerQuery = route.intent.candidatesPerQuery,
        spoilerLockEnabled = book.spoilerLockEnabled
    )

    private suspend fun extractCurrentChapterText(book: BookEntity): String? {
        val localFilePath = book.localFilePath ?: return null
        return epubTextExtractor.extractChapter(localFilePath, book.lastChapterIndex)?.text
    }
}
