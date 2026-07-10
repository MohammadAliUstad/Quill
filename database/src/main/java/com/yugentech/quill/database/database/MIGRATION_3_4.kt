package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create a new temporary table without the 'sources' column
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

        // 2. Copy the data from the old table to the new table
        db.execSQL(
            """
            INSERT INTO `aira_messages_new` (`id`, `bookId`, `role`, `content`, `timestamp`)
            SELECT `id`, `bookId`, `role`, `content`, `timestamp` FROM `aira_messages`
            """.trimIndent()
        )

        // 3. Remove the old table
        db.execSQL("DROP TABLE `aira_messages`")

        // 4. Rename the new table to the original name
        db.execSQL("ALTER TABLE `aira_messages_new` RENAME TO `aira_messages`")
        
        // 5. Create index
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_aira_messages_bookId` ON `aira_messages` (`bookId`)")
    }
}
