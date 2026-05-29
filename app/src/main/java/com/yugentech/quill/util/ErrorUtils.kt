package com.yugentech.quill.util

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException, is ConnectException -> 
            "No internet connection.\nPlease check your network."
        is SocketTimeoutException, is HttpRequestTimeoutException -> 
            "Connection timed out.\nPlease try again."
        is ResponseException -> 
            "The server is having trouble.\nPlease try again later."
        else -> "Something went wrong.\nPlease try again."
    }
}
