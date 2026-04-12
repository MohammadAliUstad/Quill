package com.yugentech.quill.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Fts4(
    contentEntity = BookChunkEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(
    tableName = "book_chunks_fts"
)
data class BookChunkFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,
    val text: String
)