package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    // Get highlights as a Flow so the UI automatically updates when a new one is added
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getHighlightsForBookFlow(bookId: String): Flow<List<HighlightEntity>>

    // Get highlights as a one-shot read
    @Query("SELECT * FROM highlights WHERE bookId = :bookId")
    suspend fun getHighlightsForBook(bookId: String): List<HighlightEntity>

    // Get all highlights across all books (useful for a "Notes" tab later)
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun getAllHighlightsFlow(): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("UPDATE highlights SET note = :note WHERE id = :highlightId")
    suspend fun updateNote(highlightId: String, note: String)

    @Query("UPDATE highlights SET colorInt = :colorInt WHERE id = :highlightId")
    suspend fun updateColor(highlightId: String, colorInt: Int)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlight(highlightId: String)
    
    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteAllHighlightsForBook(bookId: String)

    // A one-off read of every highlight, for writing a guest's backup file
    @Query("SELECT * FROM highlights")
    suspend fun getAllHighlightsOnce(): List<HighlightEntity>

    // Which of these highlight IDs are already on this phone, so an import skips them
    @Query("SELECT id FROM highlights WHERE id IN (:highlightIds)")
    suspend fun getExistingIds(highlightIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHighlights(highlights: List<HighlightEntity>)
}
