package com.yugentech.quill.ui.tabs.sourcesScreen.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.ui.shared.scrim.rememberSmoothScrimBrush
import com.yugentech.quill.ui.tabs.sourcesScreen.components.CatalogInfo
import com.yugentech.quill.ui.tabs.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportFromDeviceCard
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportStatusSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.LargeCatalogCard
import com.yugentech.quill.ui.tabs.sourcesScreen.components.SourcesParallaxBackground
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSourceClick: (BookSource) -> Unit,
    onLocalFilesClick: () -> Unit,
    viewModel: SourcesViewModel
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importResults by viewModel.importResults.collectAsStateWithLifecycle()

    var showFilePickerSheet by remember { mutableStateOf(false) }

    val libraryCounts by viewModel.libraryCounts.collectAsStateWithLifecycle()
    val coverPreviews by viewModel.coverPreviews.collectAsStateWithLifecycle()
    val sourcesDown by viewModel.sourcesDown.collectAsStateWithLifecycle()

    val catalogs = listOf(
        CatalogInfo(
            source = BookSource.STANDARD_EBOOKS,
            title = stringResource(R.string.source_standard_name),
            subtitle = stringResource(R.string.source_standard_subtitle),
            description = stringResource(R.string.source_standard_description),
            icon = Icons.Default.AutoStories,
            shape = MaterialShapes.SoftBurst.toShape(),
            stats = listOf(stringResource(R.string.source_standard_stat_books), stringResource(R.string.source_standard_stat_typeset)),
            containerColor = { MaterialTheme.colorScheme.primaryContainer },
            contentColor = { MaterialTheme.colorScheme.onPrimaryContainer },
            buttonContainerColor = { MaterialTheme.colorScheme.primary },
            buttonContentColor = { MaterialTheme.colorScheme.onPrimary },
            buttonText = stringResource(R.string.browse_catalog),
        ),
        CatalogInfo(
            source = BookSource.GUTENBERG,
            title = stringResource(R.string.source_gutenberg_name),
            subtitle = stringResource(R.string.source_gutenberg_subtitle),
            description = stringResource(R.string.source_gutenberg_description),
            icon = Icons.Default.Public,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            stats = listOf(stringResource(R.string.source_gutenberg_stat_books), stringResource(R.string.source_gutenberg_stat_languages)),
            containerColor = { MaterialTheme.colorScheme.secondaryContainer },
            contentColor = { MaterialTheme.colorScheme.onSecondaryContainer },
            buttonContainerColor = { MaterialTheme.colorScheme.secondary },
            buttonContentColor = { MaterialTheme.colorScheme.onSecondary },
            buttonText = stringResource(R.string.explore_collection),
        )
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Same treatment as the Library tab: a transparent bar over the content, with a
            // surface gradient that fades in once the content scrolls underneath.
            val surfaceColor = MaterialTheme.colorScheme.surface
            val scrimBrush = rememberSmoothScrimBrush(surfaceColor)

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (scrollState.value / 100f).coerceIn(0f, 1f)
                        }
                        .background(scrimBrush)
                )

                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.tab_sources),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SourcesParallaxBackground(
                scrollOffset = scrollState.value,
                headerHeight = 420.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Carries its own "Your files" label beside the illustration.
                ImportFromDeviceCard(
                    isImporting = isImporting,
                    importedCount = libraryCounts[BookSource.USER_IMPORTED] ?: 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        showFilePickerSheet = true
                    },
                )

                SectionLabel(text = stringResource(R.string.free_catalogues))

                catalogs.forEachIndexed { index, catalog ->
                    LargeCatalogCard(
                        catalog = catalog,
                        libraryCount = libraryCounts[catalog.source] ?: 0,
                        coverUrls = coverPreviews[catalog.source].orEmpty(),
                        isUnderMaintenance = catalog.source in sourcesDown,
                        backAfter = sourcesDown[catalog.source],
                        // Stagger the cards so their cover stacks don't shuffle in lockstep.
                        shuffleOffsetMillis = index * 1_600L,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onSourceClick(catalog.source)
                        },
                    )
                }
            }
        }

        if (showFilePickerSheet) {
            FilePickerBottomSheet(
                onDismiss = { showFilePickerSheet = false },
                onFilesSelected = { uris ->
                    showFilePickerSheet = false
                    viewModel.importFiles(context, uris)
                },
            )
        }

        // We check if there are results instead of using a separate boolean flag
        if (importResults.isNotEmpty()) {
            ImportStatusSheet(
                results = importResults,
                onDismiss = {
                    val hasSuccess = importResults.any { it is ImportResult.Success }
                    // Clear the state in ViewModel to remove the sheet
                    viewModel.clearResults()
                    if (hasSuccess) {
                        onLocalFilesClick()
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
    )
}
