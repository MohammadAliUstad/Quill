package com.yugentech.quill.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/** The app's current locale, read so that a language switch recomposes whatever uses it. */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
