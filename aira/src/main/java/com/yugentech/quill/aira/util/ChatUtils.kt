package com.yugentech.quill.aira.util

import com.yugentech.quill.database.entity.AiraMessageEntity
import com.yugentech.quill.database.entity.AiraMessageRole

object ChatUtils {
    fun formatHistory(history: List<AiraMessageEntity>): List<Map<String, Any>> =
        history.map { msg ->
            mapOf(
                "role" to when (msg.role) {
                    AiraMessageRole.USER -> "user"
                    AiraMessageRole.AIRA -> "model"
                },
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }
}
