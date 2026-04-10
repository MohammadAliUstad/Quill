package com.yugentech.quill.billing.model

object ProductIds {
    const val QUILL_PRO = "quill_pro_monthly"
    const val DONATION_COFFEE = "donation_coffee"
    const val DONATION_LUNCH = "donation_lunch"

    val subs = listOf(QUILL_PRO)
    val tips = listOf(DONATION_COFFEE, DONATION_LUNCH)
}