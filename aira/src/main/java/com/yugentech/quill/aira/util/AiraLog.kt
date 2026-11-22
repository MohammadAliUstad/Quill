package com.yugentech.quill.aira.util

import com.google.firebase.functions.FirebaseFunctionsException
import timber.log.Timber

/**
 * Aira's logs, all under one tag so a Logcat filter of `tag:Aira` shows every question, quick
 * action and failure, from the reader's peek bar to the chat screen.
 */
object AiraLog {
    private const val TAG = "Aira"

    fun w(message: String) = Timber.tag(TAG).w(message)

    /** A failure, with the Cloud Function's error code and details when the call was what failed. */
    fun failure(where: String, e: Throwable) {
        val functionError = (e as? FirebaseFunctionsException)?.let {
            " [function code=${it.code} details=${it.details}]"
        }.orEmpty()
        Timber.tag(TAG).e(e, "$where failed: ${e.javaClass.simpleName}: ${e.message}$functionError")
    }
}
