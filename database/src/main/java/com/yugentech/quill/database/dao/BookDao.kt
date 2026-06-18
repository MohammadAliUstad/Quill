package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookStorageBreakdown
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.database.view.LibraryBookView
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query(
        """
        SELECT b.* FROM books b 
        LEFT JOIN book_indexing_state i ON b.id = i.bookId
        WHERE b.downloadStatus = 'DOWNLOADED' 
        AND (i.isComplete IS NULL OR i.isComplete = 0)
    """
    )
    suspend fun getUnindexedDownloadedBooks(): List<BookEntity>

    @Query("SELECT * FROM books")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM books WHERE progressPercent >= 0.9")
    fun getFinishedBooksCountFlow(): Flow<Int>

    @Query("SELECT * FROM books WHERE isSynced = 0")
    suspend fun getUnsyncedBooks(): List<BookEntity>

    @Query("UPDATE books SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllBooksAsSynced()

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE id = :bookId)")
    suspend fun hasBook(bookId: String): Boolean

    @Query("SELECT * FROM library_view WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT 1")
    fun getLastReadBook(): Flow<LibraryBookView?>

    @Query("UPDATE books SET lastReadTime = 0, isSynced = 0 WHERE id = :bookId")
    suspend fun removeFromRecent(bookId: String)

    @Query("SELECT * FROM library_view WHERE lastReadTime > 0 ORDER BY lastReadTime DESC")
    fun getReadingHistory(): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteBooks(): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE userCategory = :shelf ORDER BY addedAt DESC")
    fun getBookShelf(shelf: String = "Shelf"): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE userCategory = :category ORDER BY addedAt DESC")
    fun getBooksByCategory(category: String): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookEntityFlow(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookEntity(bookId: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("UPDATE books SET isFavorite = :isFavorite, isSynced = 0 WHERE id = :bookId")
    suspend fun updateFavorite(bookId: String, isFavorite: Boolean)

    @Query("UPDATE books SET userCategory = :category, isSynced = 0 WHERE id = :bookId")
    suspend fun updateCategory(bookId: String, category: String)

    @Query("UPDATE books SET downloadStatus = :status, downloadError = :error WHERE id = :bookId")
    suspend fun updateDownloadStatus(bookId: String, status: DownloadStatus, error: String? = null)

    @Query("UPDATE books SET spoilerLockEnabled = :enabled WHERE id = :bookId")
    suspend fun updateSpoilerLock(bookId: String, enabled: Boolean)

    @Query("UPDATE books SET progressPercent = :progress, lastChapterTitle = :chapterTitle, lastChapterIndex = :chapterIndex, lastLocatorJson = :locatorJson, lastReadTime = :readTime, isSynced = 0 WHERE id = :bookId")
    suspend fun updateReadingProgress(
        bookId: String,
        progress: Float,
        chapterTitle: String?,
        chapterIndex: Int,
        locatorJson: String?,
        readTime: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE books 
        SET progressPercent = 0.0, 
            lastChapterTitle = NULL, 
            lastChapterIndex = 0, 
            lastScrollPosition = 0, 
            lastLocatorJson = NULL, 
            lastReadTime = 0,
            isSynced = 0 
        WHERE id = :bookId
    """
    )
    suspend fun resetReadingProgress(bookId: String)

    @Query("UPDATE books SET localFilePath = NULL, downloadStatus = 'NOT_DOWNLOADED', chapters = :emptyChapters, fileSizeBytes = 0 WHERE id = :bookId")
    suspend fun removeDownload(
        bookId: String,
        emptyChapters: List<Chapter> = emptyList()
    )

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("SELECT * FROM books WHERE downloadStatus = 'DOWNLOADED' ORDER BY fileSizeBytes DESC")
    fun getDownloadedBooksBySize(): Flow<List<BookEntity>>

    @Query(
        """
    SELECT
        b.id                            AS bookId,
        b.fileSizeBytes                 AS fileSizeBytes,
        COALESCE(SUM(
            LENGTH(c.text) + LENGTH(c.embedding)
        ), 0)                           AS chunksBytes,
        COALESCE(msg.msgBytes, 0)       AS messagesBytes
    FROM books b
    LEFT JOIN book_chunks c ON c.bookId = b.id
    LEFT JOIN (
        SELECT bookId, SUM(LENGTH(content)) AS msgBytes
        FROM aira_messages
        GROUP BY bookId
    ) msg ON msg.bookId = b.id
    WHERE b.downloadStatus = 'DOWNLOADED'
    GROUP BY b.id
"""
    )
    fun getBookStorageBreakdown(): Flow<List<BookStorageBreakdown>>
}