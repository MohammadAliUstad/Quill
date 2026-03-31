package com.yugentech.quill.aira.rag

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookChunkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class BookIndexingWorker(
    context: Context,
    params: WorkerParameters,
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val embeddingEngine: EmbeddingEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val bookId = inputData.getString(KEY_BOOK_ID)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "Missing BOOK_ID input"))

        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("▶ END-TO-END STREAMING INDEX STARTED")
        Timber.tag(TAG).d("▶ Book ID: $bookId")
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        val startTime = System.currentTimeMillis()

        try {
            val bookEntity = bookDao.getBookEntity(bookId)
            val filePath = bookEntity?.localFilePath

            if (filePath == null) {
                Timber.tag(TAG).e("✗ No local file path found for bookId=$bookId")
                return@withContext Result.failure(workDataOf(KEY_ERROR to "No local file path found"))
            }

            chunkDao.deleteChunksForBook(bookId)
            setProgress(workDataOf(KEY_PHASE to PHASE_PROCESSING, KEY_PROGRESS to 5))

            val chunkChannel = Channel<ChunkingStrategy.TextChunk>(capacity = 100)
            val entityChannel = Channel<BookChunkEntity>(capacity = 100)

            var totalChunksGenerated = 0
            var embeddedCount = 0
            val batchToSave = mutableListOf<BookChunkEntity>()

            // 1. PRODUCER
            val extractorJob = launch(Dispatchers.IO) {
                val extractor = EpubTextExtractor(applicationContext)
                extractor.extractStream(filePath)
                    .onEach { chapter ->
                        val chunks = ChunkingStrategy.chunk(chapter)
                        totalChunksGenerated += chunks.size
                        chunks.forEach { chunkChannel.send(it) }
                    }
                    .collect()

                chunkChannel.close()
            }

            // 2. TRANSFORMERS
            val concurrencyLevel = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

            val embedders = (1..concurrencyLevel).map { workerId ->
                launch(Dispatchers.Default) {
                    for (chunk in chunkChannel) {
                        val vector = embeddingEngine.embed(chunk.text)
                        if (vector != null) {
                            entityChannel.send(
                                BookChunkEntity(
                                    bookId = bookId,
                                    chapterIndex = chunk.chapterIndex,
                                    chapterTitle = chunk.chapterTitle,
                                    chunkIndex = chunk.chunkIndex,
                                    text = chunk.text,
                                    embedding = vector
                                )
                            )
                        } else {
                            Timber.tag(TAG).w("✗ Embedding failed for chunk ${chunk.chapterIndex}:${chunk.chunkIndex}")
                        }
                    }
                }
            }

            // Wait for transformers, then close entity channel
            launch(Dispatchers.Default) {
                embedders.joinAll()
                entityChannel.close()
            }

            // 3. CONSUMER
            for (entity in entityChannel) {
                batchToSave.add(entity)
                embeddedCount++

                if (batchToSave.size >= EMBEDDING_BATCH_SIZE) {
                    chunkDao.insertChunks(batchToSave)
                    batchToSave.clear()

                    val progress = (20 + (embeddedCount * 0.5f)).coerceAtMost(95f).toInt()
                    setProgress(
                        workDataOf(
                            KEY_PHASE to PHASE_PROCESSING,
                            KEY_PROGRESS to progress,
                            KEY_EMBEDDED_COUNT to embeddedCount
                        )
                    )
                }
            }

            // Flush remainder
            if (batchToSave.isNotEmpty()) {
                chunkDao.insertChunks(batchToSave)
            }

            extractorJob.join()

            setProgress(workDataOf(KEY_PHASE to PHASE_DONE, KEY_PROGRESS to 100))

            val totalMs = System.currentTimeMillis() - startTime
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("✓ INDEXING COMPLETE")
            Timber.tag(TAG).d("▶ Success   : $embeddedCount / $totalChunksGenerated chunks indexed")
            Timber.tag(TAG).d("▶ Total Time: ${totalMs}ms (${totalMs / 1000}s)")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(workDataOf(KEY_CHUNK_COUNT to embeddedCount, KEY_PHASE to PHASE_DONE))

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "✗ FATAL ERROR during indexing")
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        private const val TAG = "QuillIndexing"

        const val KEY_BOOK_ID = "BOOK_ID"
        const val KEY_PHASE = "PHASE"
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_EMBEDDED_COUNT = "EMBEDDED_COUNT"
        const val KEY_CHUNK_COUNT = "CHUNK_COUNT"
        const val KEY_ERROR = "ERROR"

        const val PHASE_PROCESSING = "processing"
        const val PHASE_DONE = "done"

        private const val EMBEDDING_BATCH_SIZE = 50
    }
}