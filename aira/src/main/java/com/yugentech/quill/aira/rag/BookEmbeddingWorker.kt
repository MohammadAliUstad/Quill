package com.yugentech.quill.aira.rag

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.entity.BookChunkEntity
import com.yugentech.quill.database.entity.BookIndexingStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookEmbeddingWorker(
    context: Context,
    params: WorkerParameters,
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val indexingStateDao: BookIndexingStateDao,
    private val embeddingEngine: EmbeddingEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val bookId = inputData.getString(KEY_BOOK_ID)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "Missing BOOK_ID"))

        try {
            val existingState = indexingStateDao.getState(bookId)
            if (existingState?.isComplete == true) {
                setProgress(workDataOf(KEY_PHASE to PHASE_DONE, KEY_PROGRESS to 100))
                return@withContext Result.success(workDataOf(KEY_PHASE to PHASE_DONE))
            }

            val filePath = bookDao.getBookEntity(bookId)?.localFilePath
                ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "No local file path found"))

            val resumeFromChapter = (existingState?.lastCompletedChapterIndex ?: -1) + 1

            if (resumeFromChapter == 0) {
                chunkDao.deleteChunksForBook(bookId)
            }

            setProgress(workDataOf(KEY_PHASE to PHASE_PROCESSING, KEY_PROGRESS to 5))

            val chunkChannel = Channel<ChunkingStrategy.TextChunk>(100)
            val entityChannel = Channel<BookChunkEntity>(50)
            val concurrencyLevel = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

            val totalChapters = withContext(Dispatchers.IO) {
                EpubTextExtractor(applicationContext).countChapters(filePath)
            }

            val extractorJob = launch(Dispatchers.IO) {
                EpubTextExtractor(applicationContext)
                    .extractStream(filePath)
                    .onEach { chapter ->
                        if (chapter.chapterIndex < resumeFromChapter) return@onEach
                        ChunkingStrategy.chunk(chapter).forEach { chunkChannel.send(it) }
                    }
                    .collect()
                chunkChannel.close()
            }

            val embedders = (1..concurrencyLevel).map {
                launch(Dispatchers.Default) {
                    for (chunk in chunkChannel) {
                        embeddingEngine.embed(chunk.text)?.let { vector ->
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
                        }
                    }
                }
            }

            launch(Dispatchers.Default) {
                embedders.joinAll()
                entityChannel.close()
            }

            var embeddedCount = 0
            val batchToSave = mutableListOf<BookChunkEntity>()
            var lastSeenChapterIndex = -1
            var highestCheckpointedChapter = resumeFromChapter - 1

            for (entity in entityChannel) {
                if (lastSeenChapterIndex >= 0 && entity.chapterIndex != lastSeenChapterIndex) {
                    if (batchToSave.isNotEmpty()) {
                        chunkDao.insertChunks(batchToSave)
                        batchToSave.clear()
                    }

                    if (lastSeenChapterIndex > highestCheckpointedChapter) {
                        highestCheckpointedChapter = lastSeenChapterIndex

                        indexingStateDao.upsertState(
                            BookIndexingStateEntity(
                                bookId = bookId,
                                lastCompletedChapterIndex = lastSeenChapterIndex,
                                isComplete = false
                            )
                        )

                        val chapterProgress = if (totalChapters > 0) {
                            ((lastSeenChapterIndex.toFloat() / totalChapters.toFloat()) * 85f + 10f)
                                .coerceIn(10f, 95f).toInt()
                        } else 10

                        setProgress(
                            workDataOf(
                                KEY_PHASE to PHASE_PROCESSING,
                                KEY_PROGRESS to chapterProgress
                            )
                        )
                    }
                }

                batchToSave.add(entity)
                lastSeenChapterIndex = entity.chapterIndex
                embeddedCount++

                if (batchToSave.size >= EMBEDDING_BATCH_SIZE) {
                    chunkDao.insertChunks(batchToSave)
                    batchToSave.clear()
                }
            }

            if (batchToSave.isNotEmpty()) {
                chunkDao.insertChunks(batchToSave)
            }

            extractorJob.join()

            indexingStateDao.upsertState(
                BookIndexingStateEntity(
                    bookId = bookId,
                    lastCompletedChapterIndex = lastSeenChapterIndex,
                    isComplete = true
                )
            )

            setProgress(workDataOf(KEY_PHASE to PHASE_DONE, KEY_PROGRESS to 100))

            Result.success(workDataOf(KEY_CHUNK_COUNT to embeddedCount, KEY_PHASE to PHASE_DONE))

        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        const val KEY_BOOK_ID = "BOOK_ID"
        const val KEY_PHASE = "PHASE"
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_CHUNK_COUNT = "CHUNK_COUNT"
        const val KEY_ERROR = "ERROR"

        const val PHASE_PROCESSING = "processing"
        const val PHASE_DONE = "done"

        private const val EMBEDDING_BATCH_SIZE = 50
    }
}