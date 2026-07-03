package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove `isLifetime` from quotas (SQLite doesn't support DROP COLUMN)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quotas_new` (
                `userId` TEXT NOT NULL,
                `queriesUsed` INTEGER NOT NULL,
                `queriesLimit` INTEGER NOT NULL,
                `resetAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `quotas_new` SELECT `userId`, `queriesUsed`, `queriesLimit`, `resetAtMillis` FROM `quotas`"
        )
        db.execSQL("DROP TABLE `quotas`")
        db.execSQL("ALTER TABLE `quotas_new` RENAME TO `quotas`")

        // Remove `sources` from aira_messages
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `aira_messages_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bookId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `aira_messages_new` SELECT `id`, `bookId`, `role`, `content`, `timestamp` FROM `aira_messages`"
        )
        db.execSQL("DROP TABLE `aira_messages`")
        db.execSQL("ALTER TABLE `aira_messages_new` RENAME TO `aira_messages`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_aira_messages_bookId` ON `aira_messages` (`bookId`)")

        // Add `downloadError` to books
        db.execSQL("ALTER TABLE `books` ADD COLUMN `downloadError` TEXT")

        // Rebuild library_view to include downloadError
        db.execSQL("DROP VIEW IF EXISTS `library_view`")
        db.execSQL(
            """
            CREATE VIEW `library_view` AS
            SELECT id, title, author, coverUrl, downloadStatus, isFavorite,
                   userCategory, progressPercent, lastReadTime, addedAt,
                   totalPages, lastChapterTitle, downloadError
            FROM books
            """.trimIndent()
        )
    }
}
