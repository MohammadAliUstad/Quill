package com.yugentech.quill.aira.rag

import android.content.Context
import org.json.JSONObject
import timber.log.Timber

class WordPieceTokenizer(
    context: Context,
    assetFileName: String = "tokenizer.json"
) {
    private val vocab = HashMap<String, Int>(32000)

    init {
        try {
            val raw = context.assets.open(assetFileName).bufferedReader().readText()
            val root = JSONObject(raw)
            val model = root.getJSONObject("model")
            val vocabObj = model.getJSONObject("vocab")

            vocabObj.keys().forEach { token ->
                vocab[token] = vocabObj.getInt(token)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize WordPieceTokenizer from assets")
        }
    }

    fun tokenize(text: String): LongArray {
        val tokens = ArrayList<Long>(128)
        tokens.add(CLS_ID.toLong())

        val normalized = normalizeText(text)

        for (word in normalized.split(WHITESPACE_REGEX)) {
            if (word.isEmpty()) continue

            if (word.length > MAX_INPUT_CHARS_PER_WORD) {
                tokens.add(UNK_ID.toLong())
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
                    val substr = if (start == 0) {
                        word.substring(start, end)
                    } else {
                        "##${word.substring(start, end)}"
                    }

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

            if (isBad) {
                tokens.add(UNK_ID.toLong())
            } else {
                tokens.addAll(subTokens)
            }

            if (tokens.size >= MAX_SEQ_LEN - 1) break
        }

        tokens.add(SEP_ID.toLong())
        return tokens.toLongArray()
    }

    private fun normalizeText(text: String): String {
        var normalized = text
            .replace("\u00AD", "")
            .replace("\u2014", " ")
            .replace("\u2013", " ")
            .lowercase()
            .trim()

        normalized = normalized.replace(PUNCTUATION_REGEX, " $1 ")
        return normalized.trim()
    }

    companion object {
        private const val UNK_ID = 100
        private const val CLS_ID = 101
        private const val SEP_ID = 102
        private const val MAX_INPUT_CHARS_PER_WORD = 100
        private const val MAX_SEQ_LEN = 512

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val PUNCTUATION_REGEX = Regex("([.,!?;:\"()'\\[\\]])")
    }
}