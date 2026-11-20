package com.yugentech.quill.reader.localization

import android.content.Context
import androidx.annotation.StringRes
import com.yugentech.theme.locale.LocaleContext

/**
 * The reader's strings for code that has no UI context of its own (its view models), as the
 * app's AppStrings: always in the language picked in the app, even after a switch below
 * Android 13. Prefer `stringResource` wherever the text is shown from Compose.
 */
object ReaderStrings {

    private lateinit var app: Context

    /** Called once from the Application's onCreate. */
    fun init(context: Context) {
        app = context.applicationContext
    }

    fun get(@StringRes id: Int, vararg args: Any): String = LocaleContext.localized(app).resources.getString(id, *args)
}
