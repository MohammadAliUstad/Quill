package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.AiraMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiraMessageDao {

    @Query("SELECT COUNT(*) FROM aira_messages WHERE role = 'USER'")
    fun getTotalUserQuestionsFlow(): Flow<Int>

    @Query("SELECT bookId, COUNT(*) as count FROM aira_messages WHERE role = 'USER' GROUP BY bookId")
    fun getQuestionsPerBookFlow(): Flow<List<BookQuestionCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiraMessageEntity)

    @Query("SELECT * FROM aira_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getMessagesForBook(bookId: String): Flow<List<AiraMessageEntity>>

    @Query("SELECT * FROM aira_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getMessagesForBookOnce(bookId: String): List<AiraMessageEntity>

    @Query("""
    SELECT * FROM (
        SELECT * FROM aira_messages 
        WHERE bookId = :bookId 
        ORDER BY timestamp DESC 
        LIMIT 6
    ) ORDER BY timestamp ASC
""")
    suspend fun getRecentMessagesForBook(bookId: String): List<AiraMessageEntity>

    @Query("DELETE FROM aira_messages WHERE bookId = :bookId")
    suspend fun clearMessagesForBook(bookId: String)

    @Query("DELETE FROM aira_messages")
    suspend fun clearAllMessages()
}

data class BookQuestionCount(
    val bookId: String,
    val count: Int
)