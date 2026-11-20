package com.yugentech.theme.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The part of the app's language setting every module needs: putting a context into the
 * language picked in the app. Here so the reader (its own module and activity) uses it as the
 * app does; the app's AppLocale stores the pick and switches it.
 *
 * From Android 13 the system applies the per-app language to every activity itself, so this
 * only does anything below 13, where the pick lives in the app's own preferences.
 */
object LocaleContext {

    /** Where the pick is kept below Android 13 (written by the app's AppLocale). */
    const val PREFS = "app_locale"
    const val KEY_TAG = "language_tag"

    /** The tag picked in the app below Android 13, or null when it follows the phone. */
    fun storedTag(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)

    /**
     * [base] in the picked language, for `attachBaseContext` of the app and of each activity.
     * Below Android 13 only; from 13 the system applies the per-app language itself.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = storedTag(base)
        if (tag == null) {
            // Following the phone: undo a default left behind by an earlier pick
            Locale.setDefault(systemLocale(base))
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        // Dates and numbers formatted with the default locale follow along too
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(configuration)
    }

    /**
     * [app] in the picked language, for strings looked up away from any screen (the reader's and
     * Aira's). Unlike [wrap] it changes nothing, and below Android 13 the wrapped context is built
     * once and reused until the pick or the app's configuration changes. The app's AppLocale
     * does the same for AppStrings.
     */
    fun localized(app: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return app
        val tag = storedTag(app) ?: return app
        val current = app.resources.configuration
        cachedLocalized?.let { if (it.tag == tag && it.base == current) return it.context }
        val configuration = Configuration(current).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        return app.createConfigurationContext(configuration).also {
            cachedLocalized = LocalizedContext(tag, Configuration(current), it)
        }
    }

    private class LocalizedContext(val tag: String, val base: Configuration, val context: Context)

    @Volatile
    private var cachedLocalized: LocalizedContext? = null

    /**
     * The phone's own language.
     *
     * From Android 13 this asks [LocaleManager.getSystemLocales]: once a per-app language is set,
     * the system applies it to this process's copy of the system resources too, so
     * [Resources.getSystem] would report the picked language instead of the phone's.
     * Below 13 nothing touches the system resources (a pick only updates the app's and the
     * activity's), so they still hold the phone's language. Never [Locale.getDefault], which
     * follows the pick.
     */
    fun systemLocale(context: Context): Locale {
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).systemLocales
        } else {
            Resources.getSystem().configuration.locales
        }
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
}
