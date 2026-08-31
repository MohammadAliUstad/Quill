package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.VisualEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisualDao {

    @Query("SELECT * FROM visuals WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getVisualsForBookFlow(bookId: String): Flow<List<VisualEntity>>

    @Query("SELECT * FROM visuals WHERE bookId = :bookId")
    suspend fun getVisualsForBook(bookId: String): List<VisualEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisual(visual: VisualEntity)

    @Query("DELETE FROM visuals WHERE id = :visualId")
    suspend fun deleteVisual(visualId: String)

    @Query("DELETE FROM visuals WHERE bookId = :bookId")
    suspend fun deleteAllVisualsForBook(bookId: String)
}
