package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsForBookFlow(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteSessionsForBook(bookId: String)
}