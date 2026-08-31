package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Replace the timestamp-based reset instant with a plain calendar-day string. An empty
        // string never equals a real "yyyy-MM-dd" today value, so every existing row is treated
        // as stale on the very next check -- which is exactly what should happen, since it
        // forces an immediate resync with the server's real count instead of carrying over
        // whatever the old, drift-prone timestamp logic had left behind.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quotas_new` (
                `userId` TEXT NOT NULL,
                `queriesUsed` INTEGER NOT NULL,
                `queriesLimit` INTEGER NOT NULL,
                `lastResetDate` TEXT NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO `quotas_new` SELECT `userId`, `queriesUsed`, `queriesLimit`, '' FROM `quotas`"
        )
        db.execSQL("DROP TABLE `quotas`")
        db.execSQL("ALTER TABLE `quotas_new` RENAME TO `quotas`")
    }
}
