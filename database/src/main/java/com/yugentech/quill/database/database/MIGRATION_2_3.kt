package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create a new temporary table without the 'isLifetime' column
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

        // 2. Copy the data from the old table to the new table
        db.execSQL(
            """
            INSERT INTO `quotas_new` (`userId`, `queriesUsed`, `queriesLimit`, `resetAtMillis`)
            SELECT `userId`, `queriesUsed`, `queriesLimit`, `resetAtMillis` FROM `quotas`
            """.trimIndent()
        )

        // 3. Remove the old table
        db.execSQL("DROP TABLE `quotas`")

        // 4. Rename the new table to the original name
        db.execSQL("ALTER TABLE `quotas_new` RENAME TO `quotas`")
    }
}
