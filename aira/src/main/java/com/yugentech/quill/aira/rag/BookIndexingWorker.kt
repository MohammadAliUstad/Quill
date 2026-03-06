package com.yugentech.quill.aira.rag

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.entity.BookChunkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookIndexingWorker(
    context: Context,
    params: WorkerParameters,
    private val bookDao: BookDao,
    private val chunkDao: BookChunkDao,
    private val embeddingEngine: EmbeddingEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookId = inputData.getString(KEY_BOOK_ID)
            ?: return@withContext Result.failure(
                workDataOf(KEY_ERROR to "Missing BOOK_ID input")
            )

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "▶ INDEXING STARTED — bookId=$bookId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        val startTime = System.currentTimeMillis()

        try {
            // ── PHASE 1: Resolve file path ────────────────────────────────
            Log.d(TAG, "[1/5] Resolving file path from Room...")
            val bookEntity = bookDao.getBookEntity(bookId)
            Log.d(TAG, "      Book title   : ${bookEntity?.title ?: "NOT FOUND"}")
            Log.d(TAG, "      Book author  : ${bookEntity?.author ?: "N/A"}")
            Log.d(TAG, "      localFilePath: ${bookEntity?.localFilePath ?: "NULL"}")

            val filePath = bookEntity?.localFilePath
            if (filePath == null) {
                Log.e(TAG, "✗ FAILED: No local file path for bookId=$bookId")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "No local file path found")
                )
            }

            // ── PHASE 2: Clear previous index ────────────────────────────
            Log.d(TAG, "[2/5] Clearing previous index for bookId=$bookId")
            val previousCount = chunkDao.getChunkCount(bookId)
            chunkDao.deleteChunksForBook(bookId)
            Log.d(TAG, "      Cleared $previousCount previously indexed chunks")

            // ── PHASE 3: Extract text ─────────────────────────────────────
            Log.d(TAG, "[3/5] Extracting text from EPUB: $filePath")
            setProgress(workDataOf(KEY_PHASE to PHASE_EXTRACTING, KEY_PROGRESS to 0))
            val extractStart = System.currentTimeMillis()
            val extractor = EpubTextExtractor(applicationContext)
            val chapters = extractor.extract(filePath)
            val extractMs = System.currentTimeMillis() - extractStart

            if (chapters.isEmpty()) {
                Log.e(TAG, "✗ FAILED: No text extracted from $filePath")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "No text could be extracted from EPUB")
                )
            }

            Log.d(TAG, "      Extracted ${chapters.size} chapters in ${extractMs}ms")
            chapters.forEachIndexed { i, ch ->
                Log.d(TAG, "      Chapter[$i]: ${ch.text.length} chars — \"${ch.text.take(80).replace('\n', ' ')}...\"")
            }

            // ── PHASE 4: Chunk ────────────────────────────────────────────
            Log.d(TAG, "[4/5] Chunking ${chapters.size} chapters...")
            setProgress(workDataOf(KEY_PHASE to PHASE_CHUNKING, KEY_PROGRESS to 20))
            val chunkStart = System.currentTimeMillis()
            val allChunks = ChunkingStrategy.chunkAll(chapters)
            val totalChunks = allChunks.size
            val chunkMs = System.currentTimeMillis() - chunkStart

            if (totalChunks == 0) {
                Log.e(TAG, "✗ FAILED: Chunking produced no output")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Chunking produced no output")
                )
            }

            Log.d(TAG, "      Produced $totalChunks chunks in ${chunkMs}ms")
            Log.d(TAG, "      Avg chunk size: ${allChunks.sumOf { it.text.length } / totalChunks} chars")
            Log.d(TAG, "      Min chunk size: ${allChunks.minOf { it.text.length }} chars")
            Log.d(TAG, "      Max chunk size: ${allChunks.maxOf { it.text.length }} chars")

            // ── PHASE 5: Embed ────────────────────────────────────────────
            Log.d(TAG, "[5/5] Embedding $totalChunks chunks (batch size=$EMBEDDING_BATCH_SIZE)...")
            setProgress(workDataOf(KEY_PHASE to PHASE_EMBEDDING, KEY_PROGRESS to 30))
            val embedStart = System.currentTimeMillis()

            val entities = mutableListOf<BookChunkEntity>()
            var embedded = 0
            var failed = 0

            allChunks.chunked(EMBEDDING_BATCH_SIZE).forEachIndexed { batchIdx, batch ->
                Log.d(TAG, "      Batch[$batchIdx]: embedding ${batch.size} chunks...")
                val batchStart = System.currentTimeMillis()

                batch.forEach { chunk ->
                    val vector = embeddingEngine.embed(chunk.text)
                    if (vector != null) {
                        entities.add(
                            BookChunkEntity(
                                bookId = bookId,
                                chapterIndex = chunk.chapterIndex,
                                chapterTitle = chunk.chapterTitle,
                                chunkIndex = chunk.chunkIndex,
                                text = chunk.text,
                                embedding = vector
                            )
                        )
                        embedded++
                    } else {
                        failed++
                        Log.w(TAG, "      ✗ Embed FAILED for chunk ${chunk.chapterIndex}:${chunk.chunkIndex} (${chunk.text.length} chars)")
                    }
                }

                val batchMs = System.currentTimeMillis() - batchStart
                Log.d(TAG, "      Batch[$batchIdx] done in ${batchMs}ms — total embedded so far: $embedded/$totalChunks")

                val embeddingProgress = 30 + ((embedded.toFloat() / totalChunks) * 60).toInt()
                setProgress(
                    workDataOf(
                        KEY_PHASE to PHASE_EMBEDDING,
                        KEY_PROGRESS to embeddingProgress,
                        KEY_EMBEDDED_COUNT to embedded,
                        KEY_TOTAL_COUNT to totalChunks
                    )
                )
            }

            val embedMs = System.currentTimeMillis() - embedStart
            Log.d(TAG, "      Embedding complete in ${embedMs}ms")
            Log.d(TAG, "      Succeeded: $embedded / $totalChunks")
            Log.d(TAG, "      Failed   : $failed / $totalChunks")
            if (embedded > 0) {
                Log.d(TAG, "      Avg ms/chunk: ${embedMs / embedded}ms")
                Log.d(TAG, "      Vector dim  : ${entities.firstOrNull()?.embedding?.size ?: "N/A"}")
            }

            // ── SAVE ──────────────────────────────────────────────────────
            Log.d(TAG, "Saving $embedded chunks to Room...")
            setProgress(workDataOf(KEY_PHASE to PHASE_SAVING, KEY_PROGRESS to 90))
            val saveStart = System.currentTimeMillis()
            chunkDao.insertChunks(entities)
            val saveMs = System.currentTimeMillis() - saveStart
            Log.d(TAG, "      Saved in ${saveMs}ms")

            setProgress(workDataOf(KEY_PHASE to PHASE_DONE, KEY_PROGRESS to 100))

            val totalMs = System.currentTimeMillis() - startTime
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✓ INDEXING COMPLETE — bookId=$bookId")
            Log.d(TAG, "  Chapters : ${chapters.size}")
            Log.d(TAG, "  Chunks   : $totalChunks total → $embedded indexed, $failed failed")
            Log.d(TAG, "  Timings  : extract=${extractMs}ms, chunk=${chunkMs}ms, embed=${embedMs}ms, save=${saveMs}ms")
            Log.d(TAG, "  Total    : ${totalMs}ms (${totalMs / 1000}s)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(
                workDataOf(
                    KEY_CHUNK_COUNT to embedded,
                    KEY_PHASE to PHASE_DONE
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "✗ FATAL ERROR for bookId=$bookId: ${e.message}", e)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    companion object {
        private const val TAG = "QuillIndexing"

        const val KEY_BOOK_ID = "BOOK_ID"
        const val KEY_PHASE = "PHASE"
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_EMBEDDED_COUNT = "EMBEDDED_COUNT"
        const val KEY_TOTAL_COUNT = "TOTAL_COUNT"
        const val KEY_CHUNK_COUNT = "CHUNK_COUNT"
        const val KEY_ERROR = "ERROR"

        const val PHASE_EXTRACTING = "extracting"
        const val PHASE_CHUNKING = "chunking"
        const val PHASE_EMBEDDING = "embedding"
        const val PHASE_SAVING = "saving"
        const val PHASE_DONE = "done"

        private const val EMBEDDING_BATCH_SIZE = 50
    }
}