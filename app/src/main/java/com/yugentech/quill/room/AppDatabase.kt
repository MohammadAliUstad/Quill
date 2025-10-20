package com.yugentech.quill.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yugentech.quill.room.daos.BookDao
import com.yugentech.quill.room.daos.CategoryDao
import com.yugentech.quill.room.entities.BookEntity
import com.yugentech.quill.room.entities.CategoryEntity
import com.yugentech.quill.network.domain.LibraryBookView
import com.yugentech.quill.room.daos.CatalogDao
import com.yugentech.quill.room.entities.CatalogCacheEntity

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        CatalogCacheEntity::class
    ],
    views = [
        LibraryBookView::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BookTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun catalogDao(): CatalogDao
}