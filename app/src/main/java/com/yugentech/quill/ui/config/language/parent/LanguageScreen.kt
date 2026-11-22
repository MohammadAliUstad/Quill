package com.yugentech.quill.ui.config.language.parent

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.localization.AppLanguage
import com.yugentech.quill.localization.AppLocale
import com.yugentech.quill.ui.common.components.SectionHeader
import com.yugentech.quill.ui.config.language.components.FooterNote
import com.yugentech.quill.ui.config.language.components.LanguageRow
import com.yugentech.theme.tokens.spacing

/**
 * Settings > Language: "System default" (the phone's language, named beside it) and the ten
 * languages Quill is translated into. Each shows its own name as written in that language,
 * so anyone can find theirs whatever the app is showing now, with its name in the current
 * language under it. Picking one switches the whole app straight away.
 */
@Composable
fun LanguageScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    var selected by remember { mutableStateOf(AppLocale.current(context)) }
    val systemLanguageName = remember {
        val locale = AppLocale.systemLocale(context)
        locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    fun pick(language: AppLanguage?) {
        if (language == selected) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        selected = language
        (context as? Activity)?.let { AppLocale.set(it, language) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.language_title))
                        Text(
                            stringResource(R.string.language_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                // Ends at the navigation bar and no further
                bottom = navBarPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.m + scaffoldPadding.calculateStartPadding(layoutDirection),
                end = MaterialTheme.spacing.m + scaffoldPadding.calculateEndPadding(layoutDirection)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                SectionHeader(icon = Icons.Default.PhoneAndroid, title = stringResource(R.string.language_section_device), startPadding = 0.dp)
            }
            item {
                LanguageRow(
                    title = stringResource(R.string.language_system_default),
                    subtitle = systemLanguageName,
                    selected = selected == null,
                    index = 0,
                    count = 1,
                    onClick = { pick(null) }
                )
            }

            item {
                SectionHeader(icon = Icons.Default.Translate, title = stringResource(R.string.language_section_languages), startPadding = 0.dp)
            }
            itemsIndexed(AppLanguage.entries, key = { _, language -> language.tag }) { index, language ->
                LanguageRow(
                    title = language.nativeName,
                    subtitle = stringResource(language.displayName),
                    selected = selected == language,
                    index = index,
                    count = AppLanguage.entries.size,
                    onClick = { pick(language) }
                )
            }

            item {
                Spacer(Modifier.height(MaterialTheme.spacing.m))
                FooterNote(
                    icon = Icons.Default.Language,
                    text = stringResource(R.string.language_footer)
                )
            }
        }
    }
}
