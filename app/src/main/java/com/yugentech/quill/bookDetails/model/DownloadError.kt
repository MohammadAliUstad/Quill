package com.yugentech.quill.bookDetails.model

import androidx.annotation.StringRes
import com.yugentech.quill.R
import com.yugentech.quill.localization.UiText

/**
 * Why a download failed, as it's kept in `books.downloadError`: a code, not a sentence, so the
 * message is put into words when it's shown, in whatever language the app is in by then.
 */
object DownloadError {
    const val NOT_FOUND = "not_found"
    const val FORBIDDEN = "forbidden"
    const val UNAVAILABLE = "unavailable"
    const val OFFLINE = "offline"
    const val STORAGE = "storage"
    const val NETWORK = "network"
    const val UNEXPECTED = "unexpected"
    private const val HTTP_PREFIX = "http:"

    /** Any other HTTP status the server answered with. */
    fun http(status: Int) = "$HTTP_PREFIX$status"

    /**
     * The message for a stored [code]. Text saved by earlier versions (already a sentence) is shown
     * as it is, until the next download replaces it.
     */
    fun message(code: String): UiText {
        code.removePrefix(HTTP_PREFIX).takeIf { code.startsWith(HTTP_PREFIX) }?.toIntOrNull()?.let {
            return UiText.Res(R.string.download_error_http, it)
        }
        return resource(code)?.let { UiText.Res(it) } ?: UiText.Raw(code)
    }

    @StringRes
    private fun resource(code: String): Int? = when (code) {
        NOT_FOUND -> R.string.download_error_not_found
        FORBIDDEN -> R.string.download_error_forbidden
        UNAVAILABLE -> R.string.download_error_unavailable
        OFFLINE -> R.string.download_error_offline
        STORAGE -> R.string.download_error_storage
        NETWORK -> R.string.download_error_network
        UNEXPECTED -> R.string.download_error_unexpected
        else -> null
    }
}
