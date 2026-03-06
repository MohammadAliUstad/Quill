package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.database.view.LibraryBookView
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE id = :bookId)")
    suspend fun hasBook(bookId: String): Boolean

    // SECTION: LIBRARY (Lightweight Views)
    @Query("SELECT * FROM library_view WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT 1")
    fun getLastReadBook(): Flow<LibraryBookView?>

    @Query("SELECT * FROM library_view WHERE lastReadTime > 0 ORDER BY lastReadTime DESC")
    fun getReadingHistory(): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteBooks(): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE userCategory = :shelf ORDER BY addedAt DESC")
    fun getBookShelf(shelf: String = "Shelf"): Flow<List<LibraryBookView>>

    @Query("SELECT * FROM library_view WHERE userCategory = :category ORDER BY addedAt DESC")
    fun getBooksByCategory(category: String): Flow<List<LibraryBookView>>

    // SECTION: BOOK DETAILS (Heavy Entities & Writes)
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookEntityFlow(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookEntity(bookId: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavorite(bookId: String, isFavorite: Boolean)

    @Query("UPDATE books SET userCategory = :category WHERE id = :bookId")
    suspend fun updateCategory(bookId: String, category: String)

    @Query("UPDATE books SET downloadStatus = :status WHERE id = :bookId")
    suspend fun updateDownloadStatus(bookId: String, status: DownloadStatus)

    @Query("UPDATE books SET spoilerLockEnabled = :enabled WHERE id = :bookId")
    suspend fun updateSpoilerLock(bookId: String, enabled: Boolean)

    @Query("UPDATE books SET progressPercent = :progress, lastChapterTitle = :chapterTitle, lastChapterIndex = :chapterIndex, lastLocatorJson = :locatorJson, lastReadTime = :readTime WHERE id = :bookId")
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
            lastReadTime = 0 
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

    @Query("SELECT SUM(fileSizeBytes) FROM books WHERE downloadStatus = 'DOWNLOADED'")
    fun getTotalStorageUsed(): Flow<Long?>

    @Query("SELECT * FROM books WHERE downloadStatus = 'DOWNLOADED' ORDER BY fileSizeBytes DESC")
    fun getDownloadedBooksBySize(): Flow<List<BookEntity>>
}