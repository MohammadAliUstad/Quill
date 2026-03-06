package com.yugentech.quill.ui.tabs.sourcesScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.ui.tabs.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.utils.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportStatusSheet
import com.yugentech.quill.utils.LocalBookImporter
import com.yugentech.quill.ui.tabs.sourcesScreen.components.SourceCard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SourcesScreen(
    onSourceClick: (BookSource) -> Unit,
    onLocalFilesClick: () -> Unit
) {
    val context = LocalContext.current
    val bookDao: BookDao = koinInject()
    val scope = rememberCoroutineScope()

    var showFilePickerSheet by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importResults by remember { mutableStateOf<List<ImportResult>>(emptyList()) }
    var showResultsSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SourceCard(
                title = "Standard Ebooks",
                subtitle = "High quality public domain",
                description = "Carefully formatted and typeset public domain ebooks with professional-grade quality and modern design.",
                icon = Icons.Default.AutoStories,
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                buttonText = "Browse Catalog",
                buttonColor = MaterialTheme.colorScheme.tertiaryContainer,
                buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onSourceClick(BookSource.STANDARD_EBOOKS) }
            )
        }

        item {
            SourceCard(
                title = "Project Gutenberg",
                subtitle = "60,000+ free eBooks",
                description = "The first and largest single collection of free eBooks. Literature from around the world in multiple languages.",
                icon = Icons.Default.Public,
                iconColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                buttonText = "Explore Library",
                buttonColor = MaterialTheme.colorScheme.tertiaryContainer,
                buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onSourceClick(BookSource.GUTENBERG) }
            )
        }

        item {
            SourceCard(
                title = "My Device",
                subtitle = "PDFs & EPUBs",
                description = "Import and read your own book collection. Select PDF or EPUB files stored anywhere on your device.",
                icon = Icons.Default.PhoneAndroid,
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                buttonText = if (isImporting) "Importing..." else "Select Files",
                buttonColor = MaterialTheme.colorScheme.tertiaryContainer,
                buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { if (!isImporting) showFilePickerSheet = true }
            )
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
            }
        )
    }

    if (showResultsSheet && importResults.isNotEmpty()) {
        ImportStatusSheet(
            results = importResults,
            onDismiss = {
                showResultsSheet = false
                importResults = emptyList()
                // Navigate to library if at least one succeeded
                if (importResults.any { it is ImportResult.Success }) {
                    onLocalFilesClick()
                }
            }
        )
    }
}