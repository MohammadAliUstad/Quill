package com.yugentech.quill.aira.rag

object ChunkingStrategy {

    data class TextChunk(
        val chapterIndex: Int,
        val chapterTitle: String,
        val chunkIndex: Int,
        val text: String
    )

    private const val DEFAULT_CHUNK_SIZE = 1500
    private const val DEFAULT_OVERLAP = 250
    private val SENTENCE_ENDINGS = setOf('.', '!', '?', '…')
    private const val MAX_SNAP_BACK = 300

    fun chunk(
        chapter: ChapterText,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<TextChunk> {
        val text = chapter.text.trim()
        if (text.isBlank()) return emptyList()

        if (text.length <= chunkSize) {
            return listOf(
                TextChunk(
                    chapterIndex = chapter.chapterIndex,
                    chapterTitle = chapter.chapterTitle,
                    chunkIndex = 0,
                    text = text
                )
            )
        }

        val chunks = mutableListOf<TextChunk>()
        var start = 0
        var chunkIndex = 0

        while (start < text.length) {
            val rawEnd = (start + chunkSize).coerceAtMost(text.length)
            val end = if (rawEnd < text.length) snapToSentenceBoundary(text, rawEnd) else rawEnd

            val chunk = text.substring(start, end).trim()
            if (chunk.isNotBlank()) {
                chunks.add(
                    TextChunk(
                        chapterIndex = chapter.chapterIndex,
                        chapterTitle = chapter.chapterTitle,
                        chunkIndex = chunkIndex++,
                        text = chunk
                    )
                )
            }

            if (end >= text.length) break

            start = (end - overlap).coerceAtLeast(0)
        }

        return chunks
    }

    private fun snapToSentenceBoundary(text: String, position: Int): Int {
        val searchStart = (position - MAX_SNAP_BACK).coerceAtLeast(0)
        for (i in position downTo searchStart + 1) {
            val c = text[i - 1]
            val next = text[i]
            if (c in SENTENCE_ENDINGS && (next == ' ' || next == '\n')) return i
        }
        return position
    }
}