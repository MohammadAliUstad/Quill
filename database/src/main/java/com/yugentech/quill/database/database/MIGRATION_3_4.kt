package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Google Play is now the only source for who has Pro, so the two columns that kept copies of it
// go: `users`.`isPro`, and `quotas`.`queriesLimit` (the daily allowance, which follows from the
// plan). Both tables are rebuilt rather than altered: SQLite only learned DROP COLUMN in 3.35,
// which Android has from version 14. The statements mirror what Room generates in
// AppDatabase_Impl, so the post-migration validation passes.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
    }
}
