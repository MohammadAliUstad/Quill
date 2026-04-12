package com.yugentech.quill.ui.about.attributions.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import com.yugentech.quill.ui.about.attributions.components.AttributionCarousel
import com.yugentech.quill.ui.about.attributions.components.AttributionsTopBar
import com.yugentech.quill.ui.about.attributions.components.LibraryItem
import com.yugentech.quill.ui.about.attributions.components.LicensesContent
import com.yugentech.quill.ui.about.attributions.components.OpenSourceCard
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.quill.ui.main.components.itemShape
import com.yugentech.theme.tokens.AppConstants.GITHUB_URL
import com.yugentech.theme.tokens.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionsScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val libraries = remember { LicensesContent.libraries }
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AttributionsTopBar(
                scrollBehavior = scrollBehavior,
                onNavigateBack = onNavigateBack
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
                top = MaterialTheme.spacing.m,
                bottom = navBarPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.m + scaffoldPadding.calculateStartPadding(
                    layoutDirection
                ),
                end = MaterialTheme.spacing.m + scaffoldPadding.calculateEndPadding(layoutDirection)
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
        ) {
            item { OpenSourceCard(githubUrl = GITHUB_URL) }

            item { AttributionCarousel() }

            item {
                SectionHeader(
                    title = "Libraries We Use",
                    icon = Icons.Default.IntegrationInstructions
                )
            }

            itemsIndexed(libraries) { index, lib ->
                LibraryItem(
                    lib = lib,
                    shape = itemShape(index, libraries.size)
                )
            }
        }
    }
}