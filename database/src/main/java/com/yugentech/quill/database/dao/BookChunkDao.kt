package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.BookChunkEntity

@Dao
interface BookChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<BookChunkEntity>)

    @Query("DELETE FROM book_chunks WHERE bookId = :bookId")
    suspend fun deleteChunksForBook(bookId: String)

    @Query("SELECT * FROM book_chunks WHERE bookId = :bookId AND chapterIndex <= :maxChapterIndex ORDER BY chapterIndex ASC, chunkIndex ASC")
    suspend fun getChunksUpToChapter(bookId: String, maxChapterIndex: Int): List<BookChunkEntity>

    @Query("SELECT COUNT(*) FROM book_chunks WHERE bookId = :bookId")
    suspend fun getChunkCount(bookId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM book_chunks WHERE bookId = :bookId LIMIT 1)")
    suspend fun isBookIndexed(bookId: String): Boolean

    @Query("""
    SELECT * FROM book_chunks 
    WHERE bookId = :bookId 
    AND chapterIndex = :chapterIndex 
    AND chunkIndex BETWEEN :fromChunkIndex AND :toChunkIndex
    ORDER BY chunkIndex ASC
""")
    suspend fun getNeighborChunks(
        bookId: String,
        chapterIndex: Int,
        fromChunkIndex: Int,
        toChunkIndex: Int
    ): List<BookChunkEntity>

    @Query("""
        SELECT book_chunks.chapterIndex, book_chunks.chunkIndex 
        FROM book_chunks 
        JOIN book_chunks_fts ON book_chunks.id = book_chunks_fts.rowid 
        WHERE book_chunks_fts.text MATCH :searchQuery 
        AND book_chunks.bookId = :bookId
    """)
    suspend fun searchFts(bookId: String, searchQuery: String): List<ChunkLocationTuple>

    @Query("""
        SELECT id, chapterIndex, chunkIndex, embedding 
        FROM book_chunks 
        WHERE bookId = :bookId AND chapterIndex <= :maxChapterIndex 
        ORDER BY chapterIndex ASC, chunkIndex ASC
    """)
    suspend fun getCandidateVectors(bookId: String, maxChapterIndex: Int): List<ChunkVectorTuple>

    @Query("""
    SELECT * FROM book_chunks 
    WHERE bookId = :bookId AND chapterIndex = :chapterIndex 
    ORDER BY chunkIndex ASC
""")
    suspend fun getChunksForChapter(bookId: String, chapterIndex: Int): List<BookChunkEntity>
}

data class ChunkVectorTuple(
    val id: Long,
    val chapterIndex: Int,
    val chunkIndex: Int,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkVectorTuple

        if (id != other.id) return false
        if (chapterIndex != other.chapterIndex) return false
        if (chunkIndex != other.chunkIndex) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + chapterIndex
        result = 31 * result + chunkIndex
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

data class ChunkLocationTuple(
    val chapterIndex: Int,
    val chunkIndex: Int
)