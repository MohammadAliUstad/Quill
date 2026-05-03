package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `highlights` (
                `id` TEXT NOT NULL, 
                `bookId` TEXT NOT NULL, 
                `locatorJson` TEXT NOT NULL, 
                `colorInt` INTEGER NOT NULL, 
                `note` TEXT, 
                `createdAt` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`)"
        )

        db.execSQL(
            "ALTER TABLE `aira_messages` ADD COLUMN `sources` TEXT NOT NULL DEFAULT '[]'"
        )
    }
}