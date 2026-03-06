package com.yugentech.quill.aira.rag

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import kotlin.math.sqrt

class EmbeddingEngine(private val context: Context) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: WordPieceTokenizer? = null

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

    private fun init() {
        if (ortSession != null) return
        try {
            Log.d(TAG, "Initializing BGE EmbeddingEngine (ONNX int8)...")
            ortEnv = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            val modelBytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
            ortSession = ortEnv!!.createSession(modelBytes, options)
            tokenizer = WordPieceTokenizer(context)
            Log.d(TAG, "EmbeddingEngine initialized — output dim: 384")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EmbeddingEngine: ${e.message}", e)
        }
    }

    /**
     * Embeds [text] into a 384-dim normalized float vector.
     *
     * For queries, pass text with [BGE_QUERY_PREFIX] prepended.
     * For book chunks, pass raw text.
     */
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
            val maskTensor     = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(tokenIds.size) { 1L }), shape)
            val typeIdsTensor  = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(tokenIds.size) { 0L }), shape)

            val inputs = mapOf(
                "input_ids"      to inputIdsTensor,
                "attention_mask" to maskTensor,
                "token_type_ids" to typeIdsTensor
            )

            val results = session.run(inputs)

            // last_hidden_state shape: [1, seqLen, 384]
            // BGE uses CLS pooling: take token at index 0
            @Suppress("UNCHECKED_CAST")
            val hiddenState = results[0].value as Array<Array<FloatArray>>
            val pooled = hiddenState[0][0]

            // Close tensors before results to avoid double-free in some ORT versions
            inputIdsTensor.close()
            maskTensor.close()
            typeIdsTensor.close()
            results.close()

            Log.d(TAG, "  Pooling check — dim=${pooled.size}, sample=[${pooled.take(5).joinToString { "%.4f".format(it) }}]")

            // FIX: Directly return the normalized array here inside the try block
            return@withContext normalize(pooled)

        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
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