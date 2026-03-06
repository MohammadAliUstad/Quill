package com.yugentech.quill.aira.rag

import android.content.Context
import org.json.JSONObject

class WordPieceTokenizer(context: Context, assetFileName: String = "tokenizer.json") {

    private val vocab = HashMap<String, Int>(32000)
    private val unkId  = 100
    private val clsId  = 101
    private val sepId  = 102
    private val maxInputCharsPerWord = 100
    private val maxSeqLen = 512

    init {
        try {
            val raw = context.assets.open(assetFileName).bufferedReader().readText()
            val root = JSONObject(raw)
            val vocabObj = root.getJSONObject("model").getJSONObject("vocab")
            vocabObj.keys().forEach { token -> vocab[token] = vocabObj.getInt(token) }
        } catch (_: Exception) {
        }
    }

    fun tokenize(text: String): LongArray {
        val tokens = ArrayList<Long>(128)
        tokens.add(clsId.toLong())

        var normalized = text
            .replace("\u00AD", "")
            .replace("\u2014", " ")
            .replace("\u2013", " ")
            .lowercase()
            .trim()

        normalized = normalized.replace(Regex("([.,!?;:\"()'\\[\\]])"), " $1 ")
        normalized = normalized.trim()

        for (word in normalized.split(Regex("\\s+"))) {
            if (word.isEmpty()) continue

            if (word.length > maxInputCharsPerWord) {
                tokens.add(unkId.toLong())
                continue
            }

            var isBad = false
            var start = 0
            val subTokens = ArrayList<Long>(8)

            while (start < word.length) {
                var end = word.length
                var foundId: Int? = null
                var foundEnd = -1

                while (start < end) {
                    val substr = if (start == 0) word.substring(start, end)
                    else "##${word.substring(start, end)}"
                    val id = vocab[substr]
                    if (id != null) {
                        foundId = id
                        foundEnd = end
                        break
                    }
                    end--
                }

                if (foundId == null) {
                    isBad = true
                    break
                }

                subTokens.add(foundId.toLong())
                start = foundEnd
            }

            if (isBad) tokens.add(unkId.toLong()) else tokens.addAll(subTokens)

            if (tokens.size >= maxSeqLen - 1) break
        }

        tokens.add(sepId.toLong())
        return tokens.toLongArray()
    }
}