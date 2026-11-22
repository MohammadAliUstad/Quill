package com.yugentech.quill.aira.rag

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.BookIndexingStateDao
import com.yugentech.quill.database.entity.BookChunkEntity
import com.yugentech.quill.database.entity.BookIndexingStateEntity
import com.yugentech.quill.domain.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Indexes one book for Aira: splits it into chunks, embeds each one and saves them, so she can
 * find the passages a question is about. Resumes from the last saved chapter if it was stopped,
 * and marks the book complete only once every chunk is saved. Books are queued with [enqueue].
 */
class BookEmbeddingWorker(
    context: Context,
    params: WorkerParameters,
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val indexingStateDao: BookIndexingStateDao,
    private val embeddingEngine: EmbeddingEngine,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        // Indexing only feeds Aira, who needs an account: a guest's books aren't indexed (their
        // library is deleted on the way out, so nothing is left half-done for an account)
        if (authRepository.isGuest) return@withContext Result.success()
        val bookId = inputData.getString(KEY_BOOK_ID)
            ?: return@withContext Result.failure()

        try {
            val existingState = indexingStateDao.getState(bookId)
            if (existingState?.isComplete == true) return@withContext Result.success()

            val filePath = bookDao.getBookEntity(bookId)?.localFilePath
                ?: return@withContext Result.failure()

            val resumeFromChapter = (existingState?.lastCompletedChapterIndex ?: -1) + 1

            if (resumeFromChapter == 0) {
                chunkDao.deleteChunksForBook(bookId)
            }

            setProgress(workDataOf(KEY_PROGRESS to 5))

            val chunkChannel = Channel<ChunkingStrategy.TextChunk>(100)
            val entityChannel = Channel<BookChunkEntity>(100)
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

                        setProgress(workDataOf(KEY_PROGRESS to chapterProgress))
                    }
                }

                batchToSave.add(entity)
                lastSeenChapterIndex = entity.chapterIndex

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

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Book indexing failed")
            Result.failure()
        }
    }

    companion object {
        /** On every indexing request: the Indexing screen lists the queue by it. */
        const val TAG_ALL = "AI_INDEXING"

        /** The one queue every book is indexed through, one book at a time, in order. */
        private const val QUEUE_NAME = "global_book_processing_queue"

        /** On a book's own indexing requests, to follow or cancel them. */
        fun tagFor(bookId: String) = "$BOOK_TAG_PREFIX$bookId"

        /** The book an indexing request is for, from its tags. */
        fun bookIdOf(tags: Set<String>): String? =
            tags.firstOrNull { it.startsWith(BOOK_TAG_PREFIX) }?.removePrefix(BOOK_TAG_PREFIX)

        private const val BOOK_TAG_PREFIX = "index_"

        /**
         * Puts [bookId] at the end of the indexing queue. Every place that indexes a book goes
         * through here, so books are never indexed side by side, nor one book twice at once.
         */
        fun enqueue(workManager: WorkManager, bookId: String) {
            val request = OneTimeWorkRequestBuilder<BookEmbeddingWorker>()
                .setInputData(workDataOf(KEY_BOOK_ID to bookId))
                .addTag(tagFor(bookId))
                .addTag(TAG_ALL)
                // When indexing is cut off (the app updated or killed, or a long book reaching
                // Android's 10-minute limit for background work), it resumes from its last saved
                // chapter. Linear, not the default exponential: that doubles the wait after every
                // interruption, so a few in a row left the whole queue waiting half an hour.
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            // Appended behind the books already waiting; a queue left failed or cancelled is
            // replaced rather than blocking every book after it
            workManager.enqueueUniqueWork(QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private const val KEY_BOOK_ID = "BOOK_ID"

        /** How far through the book it is, 0-100, in the running work's progress. */
        const val KEY_PROGRESS = "PROGRESS"

        private const val EMBEDDING_BATCH_SIZE = 50
    }
}