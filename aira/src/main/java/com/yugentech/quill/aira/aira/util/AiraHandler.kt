package com.yugentech.quill.aira.aira.util

import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class AiraHandler(
    private val queryRouter: QueryRouter,
    private val airaResponder: AiraResponder,
    private val bookDao: BookDao,
    private val airaMessageDao: AiraMessageDao
) {

    fun ask(bookId: String, question: String): Flow<AiraResponse> = flow {
        val book = bookDao.getBookEntity(bookId)!!

        Timber.d("[Aira] User prompt: \"$question\"")

        val recentHistory = airaMessageDao.getRecentMessagesForBook(bookId)
        val filteredHistory = recentHistory
            .windowed(size = 2, step = 2, partialWindows = true)
            .filter { pair ->
                if (pair.size < 2) return@filter true
                val airaResponse = pair[1]
                if (airaResponse.role != AiraMessageRole.AIRA) return@filter true
                !AiraBuilder.isDeadEnd(airaResponse.content)
            }
            .flatten()

        airaMessageDao.insertMessage(
            AiraMessageEntity(
                bookId = bookId,
                role = AiraMessageRole.USER,
                content = question.trim()
            )
        )

        try {
            val route = queryRouter.routeQuery(
                question = question,
                title = book.title,
                author = book.author
            )


            when (route) {
                is QueryRoute.RagRequired -> Timber.d(
                    "[Aira] Route: RAG | ${route.queryVariations.size} queries (3 variations + hypothetical):\n${
                        route.queryVariations.joinToString("\n") { " - $it" }
                    }"
                )

                is QueryRoute.NoRagRequired -> Timber.d("[Aira] Route: General (no RAG)")
            }

            val responseFlow = when (route) {
                is QueryRoute.RagRequired -> airaResponder.respondWithRag(
                    bookId = bookId,
                    question = question,
                    route = route,
                    history = filteredHistory,
                    book = book
                )

                is QueryRoute.NoRagRequired -> airaResponder.respondGeneral(
                    question = question,
                    history = filteredHistory,
                    book = book
                )
            }

            responseFlow.collect { response ->
                emit(response)
                if (response is AiraResponse.Success) {
                    Timber.d("[Aira] Response: \"${response.text.take(600)}\"")
                    airaMessageDao.insertMessage(
                        AiraMessageEntity(
                            bookId = bookId,
                            role = AiraMessageRole.AIRA,
                            content = response.text.trim()
                        )
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "[Aira] Handler failed for bookId: $bookId")
            emit(AiraResponse.Error(resolveErrorMessage(e)))
        }
    }

    private fun resolveErrorMessage(e: Exception): String = when {
        e.message?.contains("MAX_TOKENS") == true ->
            "The answer was too long. Please try asking for a summary."

        e.message?.contains("resource-exhausted") == true ->
            "You've reached your free limit. Upgrade to Quill Pro."

        else -> "Something went wrong. Please try again."
    }
}