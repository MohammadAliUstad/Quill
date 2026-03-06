package com.yugentech.quill.database.converter

import androidx.room.TypeConverter
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.database.model.DownloadStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RoomConverters {

    @TypeConverter
    fun fromSubjectsList(value: List<String>): String {
        return AppJson.encodeToString(value)
    }

    @TypeConverter
    fun toSubjectsList(value: String): List<String> {
        return try {
            AppJson.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromChapterList(value: List<Chapter>): String {
        return AppJson.encodeToString(value)
    }

    @TypeConverter
    fun toChapterList(value: String): List<Chapter> {
        return try {
            AppJson.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromBookSource(source: BookSource): String = source.name

    @TypeConverter
    fun toBookSource(value: String): BookSource = try {
        BookSource.valueOf(value)
    } catch (_: Exception) {
        BookSource.USER_IMPORTED
    }

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (_: Exception) {
        DownloadStatus.NOT_DOWNLOADED
    }

    @TypeConverter
    fun floatArrayToByteArray(value: FloatArray): ByteArray {
        val buffer = ByteBuffer
            .allocate(value.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(value)
        return buffer.array()
    }

    @TypeConverter
    fun byteArrayToFloatArray(value: ByteArray): FloatArray {
        val buffer = ByteBuffer
            .wrap(value)
            .order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(value.size / Float.SIZE_BYTES)
        buffer.asFloatBuffer().get(floats)
        return floats
    }
}