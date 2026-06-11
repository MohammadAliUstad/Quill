package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the 'style' column with a default value of 'HIGHLIGHT'
        db.execSQL("ALTER TABLE `highlights` ADD COLUMN `style` TEXT NOT NULL DEFAULT 'HIGHLIGHT'")
    }
}
