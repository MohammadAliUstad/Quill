package com.yugentech.quill.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yugentech.quill.room.daos.BookChapterDao
import com.yugentech.quill.room.daos.CatalogDao
import com.yugentech.quill.room.daos.LibraryDao
import com.yugentech.quill.room.entities.BookChapterEntity
import com.yugentech.quill.room.entities.CatalogCacheEntity
import com.yugentech.quill.room.entities.LibraryBookEntity
import com.yugentech.quill.room.entities.UserCategoryEntity

@Database(
    entities = [
        LibraryBookEntity::class,
        CatalogCacheEntity::class,
        UserCategoryEntity::class,
        BookChapterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BookTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao
    abstract fun catalogDao(): CatalogDao
    abstract fun bookChapterDao(): BookChapterDao
}