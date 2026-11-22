package com.yugentech.quill.aira.localization

import android.content.Context
import androidx.annotation.StringRes

/**
 * Aira's own messages (errors, and the stand-in questions a chip tap records in the chat
 * history), in the language picked in the app, as the app's AppStrings.
 *
 * This module doesn't depend on the theme module, so the app hands it the way to get its
 * context in the picked language (theme's LocaleContext.localized, cached), rather than this
 * module keeping its own copy of that logic.
 */
object AiraStrings {

    private lateinit var localized: () -> Context

    /** Called once from the Application's onCreate, with the app's context in the picked language. */
    fun init(localized: () -> Context) {
        this.localized = localized
    }

    fun get(@StringRes id: Int, vararg args: Any): String = localized().resources.getString(id, *args)
}
