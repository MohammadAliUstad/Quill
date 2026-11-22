package com.yugentech.quill.utils

import com.yugentech.quill.localization.UiText
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.yugentech.quill.R

/** What went wrong, for the user: a resource, so it's in the current language whenever it's shown. */
fun Throwable.toUserFriendlyMessage(): UiText = UiText.Res(
    when (this) {
        is UnknownHostException, is ConnectException -> R.string.error_offline
        is SocketTimeoutException, is HttpRequestTimeoutException -> R.string.error_timeout
        is ResponseException -> R.string.error_server
        else -> R.string.error_unknown
    }
)
