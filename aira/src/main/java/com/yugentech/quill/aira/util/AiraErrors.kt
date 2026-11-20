package com.yugentech.quill.aira.util

import com.google.firebase.functions.FirebaseFunctionsException

object AiraErrors {
    /**
     * Whether Gemini's safety filters refused the request. The Cloud Functions mark it with
     * `failed-precondition` and `details.reason = "content_blocked"` (see functions/src/core/gemini.ts).
     * Asking again gets the same answer, so it gets its own message rather than "try again".
     */
    fun isContentBlocked(e: Throwable): Boolean {
        val functionError = e as? FirebaseFunctionsException ?: return false
        if (functionError.code != FirebaseFunctionsException.Code.FAILED_PRECONDITION) return false
        return (functionError.details as? Map<*, *>)?.get("reason") == "content_blocked"
    }
}
