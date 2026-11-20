package com.yugentech.quill.quota.model

/** Aira queries a day. The plan comes from Google Play, so the allowance is never stored. */
object QuotaLimits {
    const val FREE = 5
    const val PRO = 50

    fun of(isPro: Boolean): Int = if (isPro) PRO else FREE
}
