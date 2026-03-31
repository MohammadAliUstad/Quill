package com.yugentech.quill.aira.rag

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.LongBuffer
import kotlin.math.sqrt

class EmbeddingEngine(private val context: Context) {

    @Volatile private var ortEnv: OrtEnvironment? = null
    @Volatile private var ortSession: OrtSession? = null
    @Volatile private var tokenizer: WordPieceTokenizer? = null

    private val initMutex = Mutex()

    companion object {
        private const val TAG = "QuillEmbedding"
        private const val MODEL_FILE = "model.onnx"
        const val BGE_QUERY_PREFIX = "Represent this sentence for searching relevant passages: "

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot
        }
    }

    suspend fun init() = withContext(Dispatchers.IO) {
        if (ortSession != null) return@withContext

        initMutex.withLock {
            if (ortSession != null) return@withContext

            try {
                ortEnv = OrtEnvironment.getEnvironment()

                val options = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setIntraOpNumThreads(1)
                }

                val cachedModelFile = File(context.cacheDir, MODEL_FILE)
                if (!cachedModelFile.exists()) {
                    context.assets.open(MODEL_FILE).use { input ->
                        cachedModelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                ortSession = ortEnv!!.createSession(cachedModelFile.absolutePath, options)
                tokenizer = WordPieceTokenizer(context)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "✗ Failed to initialize EmbeddingEngine")
            }
        }
    }

    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext null

        init()

        val env = ortEnv ?: return@withContext null
        val session = ortSession ?: return@withContext null
        val tok = tokenizer ?: return@withContext null

        try {
            val tokenIds = tok.tokenize(text)
            val seqLen = tokenIds.size.toLong()
            val shape = longArrayOf(1L, seqLen)

            val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenIds), shape)
            val maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(tokenIds.size) { 1L }), shape)
            val typeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(tokenIds.size)), shape)

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to maskTensor,
                "token_type_ids" to typeIdsTensor
            )

            val results = session.run(inputs)

            @Suppress("UNCHECKED_CAST")
            val hiddenState = results[0].value as Array<Array<FloatArray>>
            val pooled = hiddenState[0][0]

            inputIdsTensor.close()
            maskTensor.close()
            typeIdsTensor.close()
            results.close()

            return@withContext normalize(pooled)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "✗ Inference failed for text: ${text.take(50)}...")
            return@withContext null
        }
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sumSq = 0f
        for (x in v) sumSq += x * x
        val norm = sqrt(sumSq)
        if (norm == 0f) return v
        return FloatArray(v.size) { i -> v[i] / norm }
    }

    fun close() {
        ortSession?.close()
        ortEnv?.close()
        ortSession = null
        ortEnv = null
    }
}