package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add the new column to the books table
        db.execSQL("ALTER TABLE `books` ADD COLUMN `downloadError` TEXT")

        // 2. Drop the existing library_view
        db.execSQL("DROP VIEW IF EXISTS `library_view`")

        // 3. Recreate the library_view with the new column included
        // We match the SQL exactly as defined in the LibraryBookView and expected by Room
        db.execSQL(
            "CREATE VIEW `library_view` AS SELECT id, title, author, coverUrl, downloadStatus, isFavorite, \n" +
            "               userCategory, progressPercent, lastReadTime, addedAt,\n" +
            "               totalPages, lastChapterTitle, downloadError \n" +
            "        FROM books"
        )
    }
}
