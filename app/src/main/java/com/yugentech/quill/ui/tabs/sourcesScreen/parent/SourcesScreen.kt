package com.yugentech.quill.ui.tabs.sourcesScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.ui.tabs.sourcesScreen.components.CatalogInfo
import com.yugentech.quill.ui.tabs.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportStatusSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.LargeCatalogCard
import com.yugentech.quill.utils.ImportResult
import com.yugentech.quill.utils.LocalBookImporter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSourceClick: (BookSource) -> Unit,
    onLocalFilesClick: () -> Unit,
) {
    val context = LocalContext.current
    val bookDao: BookDao = koinInject()
    val scope = rememberCoroutineScope()

    var showFilePickerSheet by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importResults by remember { mutableStateOf<List<ImportResult>>(emptyList()) }
    var showResultsSheet by remember { mutableStateOf(false) }

    val catalogs = listOf(
        CatalogInfo(
            source = BookSource.STANDARD_EBOOKS,
            title = "Standard Ebooks",
            subtitle = "High quality public domain",
            description = "Carefully formatted and typeset public domain ebooks with professional-grade quality and modern design.",
            icon = Icons.Default.AutoStories,
            shape = MaterialShapes.SoftBurst.toShape(),
            containerColor = { MaterialTheme.colorScheme.primaryContainer },
            contentColor = { MaterialTheme.colorScheme.onPrimaryContainer },
            buttonContainerColor = { MaterialTheme.colorScheme.onPrimaryContainer },
            buttonContentColor = { MaterialTheme.colorScheme.primaryContainer },
            buttonText = "Browse Catalog",
        ),
        CatalogInfo(
            source = BookSource.GUTENBERG,
            title = "Project Gutenberg",
            subtitle = "60,000+ free eBooks",
            description = "The first and largest single collection of free eBooks. Literature from around the world in multiple languages.",
            icon = Icons.Default.Public,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            containerColor = { MaterialTheme.colorScheme.secondaryContainer },
            contentColor = { MaterialTheme.colorScheme.onSecondaryContainer },
            buttonContainerColor = { MaterialTheme.colorScheme.onSecondaryContainer },
            buttonContentColor = { MaterialTheme.colorScheme.secondaryContainer },
            buttonText = "Explore Collection",
        ),
    )

    val localImportCatalog = CatalogInfo(
        source = BookSource.USER_IMPORTED,
        title = "Import from Device",
        subtitle = "Supports EPUB files",
        description = "Import and read your own book collection. Select EPUB files stored anywhere on your device.",
        icon = Icons.Default.FolderOpen,
        shape = MaterialShapes.Bun.toShape(),
        containerColor = { MaterialTheme.colorScheme.surfaceContainerHigh },
        contentColor = { MaterialTheme.colorScheme.onSurface },
        buttonContainerColor = { MaterialTheme.colorScheme.secondaryContainer },
        buttonContentColor = { MaterialTheme.colorScheme.onSecondaryContainer },
        buttonText = if (isImporting) "Importing..." else "Browse Device",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sources",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 8.dp
                )
        ) {

            // --- NEW: The Local Import Card is now at the very top! ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                LargeCatalogCard(
                    catalog = localImportCatalog,
                    onClick = { if (!isImporting) showFilePickerSheet = true },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- The rest of the online catalogs follow below ---
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing slightly for better rhythm
            ) {
                catalogs.forEach { catalog ->
                    LargeCatalogCard(
                        catalog = catalog,
                        onClick = { onSourceClick(catalog.source) },
                    )
                }
            }
        }
    }

    if (showFilePickerSheet) {
        FilePickerBottomSheet(
            onDismiss = { showFilePickerSheet = false },
            onFilesSelected = { uris ->
                showFilePickerSheet = false
                isImporting = true
                scope.launch {
                    val results = LocalBookImporter.importFiles(context, bookDao, uris)
                    importResults = results
                    isImporting = false
                    showResultsSheet = true
                }
            },
        )
    }

    if (showResultsSheet && importResults.isNotEmpty()) {
        ImportStatusSheet(
            results = importResults,
            onDismiss = {
                showResultsSheet = false
                importResults = emptyList()
                if (importResults.any { it is ImportResult.Success }) {
                    onLocalFilesClick()
                }
            },
        )
    }
}