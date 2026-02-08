package com.yugentech.quill.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yugentech.quill.room.daos.BookDetailsDao
import com.yugentech.quill.room.daos.CatalogDao
import com.yugentech.quill.room.daos.CategoryDao
import com.yugentech.quill.room.daos.LibraryBooksDao
import com.yugentech.quill.room.entities.BookDetailsEntity
import com.yugentech.quill.room.entities.CatalogCacheEntity
import com.yugentech.quill.room.entities.LibraryBookEntity
import com.yugentech.quill.room.entities.UserCategoryEntity

@Database(
    entities = [
        LibraryBookEntity::class,
        CatalogCacheEntity::class,
        UserCategoryEntity::class,
        BookDetailsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BookTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryBooksDao
    abstract fun catalogDao(): CatalogDao
    abstract fun bookDetailsDao(): BookDetailsDao
    abstract fun categoryDao(): CategoryDao
}