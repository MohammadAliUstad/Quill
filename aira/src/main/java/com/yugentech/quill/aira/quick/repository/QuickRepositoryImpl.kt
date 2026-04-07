package com.yugentech.quill.aira.quick.repository

import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.aira.quick.prompt.QuickPrompt
import com.yugentech.quill.aira.quick.util.QuickHandler
import com.yugentech.quill.aira.rag.RagRetriever
import com.yugentech.quill.aira.response.AiraResponse
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import kotlinx.coroutines.flow.Flow

class QuickRepositoryImpl(
    private val functions: FirebaseFunctions,
    private val ragRetriever: RagRetriever,
    private val bookChunkDao: BookChunkDao,
    private val bookDao: BookDao
) : QuickRepository {

    private val handler by lazy {
        QuickHandler(
            functions = functions,
            ragRetriever = ragRetriever,
            bookDao = bookDao,
            bookChunkDao = bookChunkDao
        )
    }

    override suspend fun handle(bookId: String, quickPrompt: QuickPrompt): Flow<AiraResponse> {
        return handler.handle(bookId, quickPrompt)
    }
}