package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 4.5.0 (the last Play Store release) ships database version 3; this takes it straight to 5.0.0's
// schema in one step.
//
// Tables are rebuilt rather than altered where a column goes: SQLite only learned DROP COLUMN in
// 3.35, which Android has from version 14. The statements mirror what Room generates in
// AppDatabase_Impl, so the post-migration validation passes.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Google Play is now the only source for who has Pro, so the two columns that kept copies
        // of it go: `users`.`isPro`, and `quotas`.`queriesLimit` (the daily allowance, which
        // follows from the plan).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `users_new` (`userId` TEXT NOT NULL, `name` TEXT, `email` TEXT, `avatarId` INTEGER, PRIMARY KEY(`userId`))"
        )
        db.execSQL(
            "INSERT INTO `users_new` SELECT `userId`, `name`, `email`, `avatarId` FROM `users`"
        )
        db.execSQL("DROP TABLE `users`")
        db.execSQL("ALTER TABLE `users_new` RENAME TO `users`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quotas_new` (`userId` TEXT NOT NULL, `queriesUsed` INTEGER NOT NULL, `lastResetDate` TEXT NOT NULL, PRIMARY KEY(`userId`))"
        )
        db.execSQL(
            "INSERT INTO `quotas_new` SELECT `userId`, `queriesUsed`, `lastResetDate` FROM `quotas`"
        )
        db.execSQL("DROP TABLE `quotas`")
        db.execSQL("ALTER TABLE `quotas_new` RENAME TO `quotas`")

        // Project Gutenberg is no longer a source. A Gutenberg book already downloaded to the
        // device stays, as one of the reader's own imported books: readable as before, with its
        // progress, highlights and Aira chats, and no longer synced (as imports aren't). One that
        // was never downloaded can't be fetched any more, so it leaves the library with everything
        // kept for it. The cached Gutenberg catalogue goes too. The rows belonging to removed
        // books are deleted by hand rather than left to ON DELETE CASCADE, which only acts while
        // foreign keys are enforced.
        val unreachable =
            "SELECT `id` FROM `books` WHERE `source` = 'GUTENBERG' " +
                "AND NOT (`downloadStatus` = 'DOWNLOADED' AND `localFilePath` IS NOT NULL)"

        listOf("reading_sessions", "highlights", "aira_messages", "visuals").forEach { table ->
            db.execSQL("DELETE FROM `$table` WHERE `bookId` IN ($unreachable)")
        }
        db.execSQL("DELETE FROM `books` WHERE `id` IN ($unreachable)")

        db.execSQL("UPDATE `books` SET `source` = 'USER_IMPORTED' WHERE `source` = 'GUTENBERG'")
        db.execSQL("DELETE FROM `catalog_cache` WHERE `source` = 'GUTENBERG'")

        // highlights: drop `note`. Nothing in the app could ever write one, so it's always empty.
        // Only rows whose book still exists are carried over, since Room runs a foreign key check
        // after migrating and would reject orphans.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `highlights_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `locatorJson` TEXT NOT NULL, `colorInt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `highlights_new` SELECT `id`, `bookId`, `locatorJson`, `colorInt`, `createdAt` FROM `highlights` WHERE `bookId` IN (SELECT `id` FROM `books`)"
        )
        db.execSQL("DROP TABLE `highlights`")
        db.execSQL("ALTER TABLE `highlights_new` RENAME TO `highlights`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_highlights_bookId` ON `highlights` (`bookId`)")
    }
}
