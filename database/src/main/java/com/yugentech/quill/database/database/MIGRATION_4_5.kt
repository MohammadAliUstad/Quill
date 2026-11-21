package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Project Gutenberg is no longer a source. A Gutenberg book already downloaded to the device
// stays, as one of the reader's own imported books: readable as before, with its progress,
// highlights and Aira chats, and no longer synced (as imports aren't). One that was never
// downloaded can't be fetched any more, so it leaves the library with everything kept for it.
// The cached Gutenberg catalogue goes too.
//
// The rows belonging to removed books are deleted by hand rather than left to ON DELETE CASCADE,
// which only acts while foreign keys are enforced.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val unreachable =
            "SELECT `id` FROM `books` WHERE `source` = 'GUTENBERG' " +
                "AND NOT (`downloadStatus` = 'DOWNLOADED' AND `localFilePath` IS NOT NULL)"

        listOf("reading_sessions", "highlights", "aira_messages", "visuals").forEach { table ->
            db.execSQL("DELETE FROM `$table` WHERE `bookId` IN ($unreachable)")
        }
        db.execSQL("DELETE FROM `books` WHERE `id` IN ($unreachable)")

        db.execSQL("UPDATE `books` SET `source` = 'USER_IMPORTED' WHERE `source` = 'GUTENBERG'")
        db.execSQL("DELETE FROM `catalog_cache` WHERE `source` = 'GUTENBERG'")
    }
}
