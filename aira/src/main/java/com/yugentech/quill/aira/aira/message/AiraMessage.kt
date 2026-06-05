package com.yugentech.quill.aira.aira.message

data class AiraMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role {
        USER,
        AIRA
    }
}
