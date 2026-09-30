package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Production (3.2.0, ai-lock) ships database version 2; this takes it straight to the current
// schema. Every statement mirrors what Room generates in AppDatabase_Impl -- including the view
// text, which Room compares character for character -- so the post-migration validation passes.
// Rows are only carried over when their book still exists, since Room runs a foreign key check
// after migrating and would reject orphans.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // quotas: `resetAtMillis` + `isLifetime` become a plain calendar-day string. An empty
        // string never equals a real "yyyy-MM-dd" today, so every row resyncs on its next check.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quotas_new` (`userId` TEXT NOT NULL, `queriesUsed` INTEGER NOT NULL, `queriesLimit` INTEGER NOT NULL, `lastResetDate` TEXT NOT NULL, PRIMARY KEY(`userId`))"
        )
        db.execSQL(
            "INSERT INTO `quotas_new` SELECT `userId`, `queriesUsed`, `queriesLimit`, '' FROM `quotas`"
        )
        db.execSQL("DROP TABLE `quotas`")
        db.execSQL("ALTER TABLE `quotas_new` RENAME TO `quotas`")

        // aira_messages: drop `sources`.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `aira_messages_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `aira_messages_new` SELECT `id`, `bookId`, `role`, `content`, `timestamp` FROM `aira_messages` WHERE `bookId` IN (SELECT `id` FROM `books`)"
        )
        db.execSQL("DROP TABLE `aira_messages`")
        db.execSQL("ALTER TABLE `aira_messages_new` RENAME TO `aira_messages`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_aira_messages_bookId` ON `aira_messages` (`bookId`)")

        // highlights: some 3.2.0 builds created this table with a `style` column that was later
        // removed without a version bump. Rebuilding with the named columns handles both shapes.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `highlights_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `locatorJson` TEXT NOT NULL, `colorInt` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `highlights_new` SELECT `id`, `bookId`, `locatorJson`, `colorInt`, `note`, `createdAt` FROM `highlights` WHERE `bookId` IN (SELECT `id` FROM `books`)"
        )
        db.execSQL("DROP TABLE `highlights`")
        db.execSQL("ALTER TABLE `highlights_new` RENAME TO `highlights`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`)")

        // books: add `downloadError`.
        db.execSQL("ALTER TABLE `books` ADD COLUMN `downloadError` TEXT")

        // visuals: new table for Visualize This images.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `visuals` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `chapterIndex` INTEGER NOT NULL, `chapterTitle` TEXT NOT NULL, `sourceText` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `locatorJson` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_visuals_bookId` ON `visuals` (`bookId`)")

        // library_view: rebuild to include `downloadError`. The text must match LibraryBookView's
        // @DatabaseView exactly (spacing and line breaks included) or Room's validation fails.
        db.execSQL("DROP VIEW IF EXISTS `library_view`")
        db.execSQL(
            "CREATE VIEW `library_view` AS SELECT id, title, author, coverUrl, downloadStatus, isFavorite, \n" +
                "               userCategory, progressPercent, lastReadTime, addedAt,\n" +
                "               totalPages, lastChapterTitle, downloadError \n" +
                "        FROM books"
        )
    }
}
