package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yugentech.quill.database.entity.BookIndexingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookIndexingStateDao {
    @Query("SELECT * FROM book_indexing_state WHERE bookId = :bookId LIMIT 1")
    suspend fun getState(bookId: String): BookIndexingStateEntity?

    @Query("SELECT COALESCE(isComplete, 0) FROM book_indexing_state WHERE bookId = :bookId")
    fun observeIsComplete(bookId: String): Flow<Boolean>

    @Upsert
    suspend fun upsertState(state: BookIndexingStateEntity)

    @Query("DELETE FROM book_indexing_state WHERE bookId = :bookId")
    suspend fun deleteState(bookId: String)
}