package com.yugentech.quill.ui.dash.screens.sourcesScreen.parent

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.dash.screens.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.ui.dash.screens.sourcesScreen.components.SourceCard

@Composable
fun SourcesScreen(
    onSourceClick: (String) -> Unit,
    onLocalFilesClick: () -> Unit // Opens file picker modal
) {
    var showFilePickerSheet by remember { mutableStateOf(false) }

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
                onClick = { onSourceClick("standard_ebooks") }
            )
        }

        // Project Gutenberg Card
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
                onClick = { onSourceClick("gutenberg") }
            )
        }

        // Local Files Card
        item {
            SourceCard(
                title = "My Device",
                subtitle = "PDFs & EPUBs",
                description = "Import and read your own book collection. Select PDF or EPUB files stored anywhere on your device.",
                icon = Icons.Default.PhoneAndroid, // or FolderOpen
                iconColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                buttonText = "Select Files",
                buttonColor = MaterialTheme.colorScheme.tertiaryContainer,
                buttonContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { showFilePickerSheet = true }
            )
        }
    }

    // File Picker Modal Bottom Sheet
    if (showFilePickerSheet) {
        FilePickerBottomSheet(
            onDismiss = { showFilePickerSheet = false },
            onFilesSelected = { files ->
                onLocalFilesClick()
                showFilePickerSheet = false
            }
        )
    }
}