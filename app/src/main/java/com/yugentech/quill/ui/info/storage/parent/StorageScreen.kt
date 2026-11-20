package com.yugentech.quill.ui.info.storage.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.R
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.storage.viewmodel.StorageViewModel
import com.yugentech.quill.ui.info.storage.components.BookStorageList
import com.yugentech.quill.ui.info.storage.components.DeleteBookDialog
import com.yugentech.quill.ui.info.storage.components.StorageHeader
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StorageScreen(
    onBackClick: () -> Unit,
    storageViewModel: StorageViewModel
) {
    val haptic = LocalHapticFeedback.current
    val uiState by storageViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.more_manage_storage), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.storage_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator()
                }
            }

            uiState.downloadedBooks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.offset(y = (-50).dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_storage),
                            contentDescription = stringResource(R.string.no_downloads_cd),
                            modifier = Modifier.size(240.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.storage_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.storage_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding() + 8.dp
                        )
                ) {
                    StorageHeader(
                        appUsedBytes = uiState.appStorageUsedBytes,
                        freeBytes = uiState.deviceFreeSpaceBytes,
                        totalBytes = uiState.deviceTotalSpaceBytes,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BookStorageList(
                        books = uiState.downloadedBooks,
                        breakdowns = uiState.bookStorageBreakdowns,
                        onDeleteClick = { book -> bookToDelete = book },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    bookToDelete?.let { book ->
        DeleteBookDialog(
            bookTitle = book.title,
            isImported = book.source == BookSource.USER_IMPORTED,
            onConfirm = {
                storageViewModel.deleteBook(book.id)
                bookToDelete = null
            },
            onDismiss = {
                bookToDelete = null
            }
        )
    }
}