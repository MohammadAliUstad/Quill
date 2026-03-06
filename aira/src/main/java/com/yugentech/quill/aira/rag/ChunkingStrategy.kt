package com.yugentech.quill.aira.rag

object ChunkingStrategy {

    data class TextChunk(
        val chapterIndex: Int,
        val chapterTitle: String,
        val chunkIndex: Int,
        val text: String
    )

    // 1500 chars ≈ 250–350 tokens — large enough to contain a complete narrative
    // beat, character introduction, or plot event as a coherent self-contained passage.
    // Change from 1500 to ~250-300 characters
    private const val DEFAULT_CHUNK_SIZE = 300
    // Reduce overlap since chunks are smaller, just enough to catch split words
    private const val DEFAULT_OVERLAP = 50

    // Period, exclamation, question mark, ellipsis only.
    // Quotes removed — they appear mid-dialogue and cause bad splits in Victorian prose.
    private val SENTENCE_ENDINGS = setOf('.', '!', '?', '…')
    private const val MAX_SNAP_BACK = 150

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

            // Start next chunk at end minus overlap — no snap on start,
            // let end-snap handle boundary alignment.
            start = (end - overlap).coerceAtLeast(0)
        }

        return chunks
    }

    fun chunkAll(
        chapters: List<ChapterText>,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<TextChunk> = chapters.flatMap { chunk(it, chunkSize, overlap) }

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