package com.yugentech.quill.database.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Lets a visual remember exactly where in the book its source passage was, the same way
        // highlights already do, so tapping it can jump the reader straight back there.
        db.execSQL("ALTER TABLE `visuals` ADD COLUMN `locatorJson` TEXT")
    }
}
