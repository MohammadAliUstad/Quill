package com.yugentech.quill.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yugentech.quill.database.converter.RoomConverters
import com.yugentech.quill.database.dao.AiraMessageDao
import com.yugentech.quill.database.dao.BookChunkDao
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.CatalogDao
import com.yugentech.quill.database.dao.CategoryCacheDao
import com.yugentech.quill.database.dao.CategoryDao
import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.BookChunkEntity
import com.yugentech.quill.database.entity.BookChunkFtsEntity
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CatalogCacheEntity
import com.yugentech.quill.database.entity.CategoryCacheEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.view.LibraryBookView

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        CatalogCacheEntity::class,
        BookChunkEntity::class,
        BookChunkFtsEntity::class,
        AiraMessageEntity::class,
        CategoryCacheEntity::class
    ],
    views = [
        LibraryBookView::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun catalogDao(): CatalogDao
    abstract fun bookChunkDao(): BookChunkDao
    abstract fun airaMessageDao(): AiraMessageDao
    abstract fun categoryCacheDao(): CategoryCacheDao
}