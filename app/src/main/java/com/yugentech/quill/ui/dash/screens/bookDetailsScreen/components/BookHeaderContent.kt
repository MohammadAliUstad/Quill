package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yugentech.quill.R
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.room.entities.DownloadStatus
import com.yugentech.quill.room.entities.LibraryBookEntity

@Composable
fun BookHeaderContent(
    book: Book,
    topPadding: Dp,
    libraryBook: LibraryBookEntity?,
    downloadStatus: DownloadStatus,
    onCategoryClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onReadClick: () -> Unit
) {
    // Track if we were previously downloading.
    // This allows us to trigger the "Download Complete" animation only when
    // transitioning from Downloading -> Downloaded, rather than every time we navigate here.
    var wasDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(downloadStatus) {
        if (downloadStatus == DownloadStatus.DOWNLOADING) {
            wasDownloading = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 16.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT: Book Cover ---
        ElevatedCard(
            modifier = Modifier
                .width(160.dp)
                .height(240.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
                defaultElevation = 12.dp
            )
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Book Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // --- RIGHT: Info & Actions Column ---
        Column(
            modifier = Modifier
                .weight(1f)
                .height(240.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Title and Author
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.25f),
                            blurRadius = 4f
                        )
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // B. Action Buttons
            Column {
                val isSaved = libraryBook != null
                val categoryLabel = if (isSaved) libraryBook.userCategory else "Category"
                val isDownloadEnabled = downloadStatus != DownloadStatus.DOWNLOADING &&
                        downloadStatus != DownloadStatus.DOWNLOADED

                // 1. Category Button
                ActionButton(
                    icon = Icons.Default.FolderOpen,
                    label = categoryLabel,
                    onClick = { if (isSaved) onCategoryClick() },
                    enabled = isSaved,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Download Button
                ActionButton(
                    icon = Icons.Default.Download, // Default icon when not downloading/downloaded
                    label = when (downloadStatus) {
                        DownloadStatus.DOWNLOADED -> "Downloaded"
                        DownloadStatus.DOWNLOADING -> "Downloading..."
                        else -> "Download"
                    },
                    onClick = { if (isDownloadEnabled) onDownloadClick() },
                    containerColor = if (downloadStatus == DownloadStatus.DOWNLOADED)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (downloadStatus == DownloadStatus.DOWNLOADED)
                        MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    customIcon = {
                        when (downloadStatus) {
                            DownloadStatus.DOWNLOADING -> {
                                // Always animate the spinner while downloading
                                AnimatedDownloadIcon(
                                    drawableResId = R.drawable.downloading_start,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            DownloadStatus.DOWNLOADED -> {
                                if (wasDownloading) {
                                    // If we just finished downloading, play the success animation
                                    AnimatedDownloadIcon(
                                        drawableResId = R.drawable.download_complete,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    // If we navigated here and it was already done, show YOUR static custom vector
                                    Icon(
                                        painter = painterResource(id = R.drawable.download_mark),
                                        contentDescription = "Downloaded",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> null // Fallback to the default 'icon' param (Download Icon)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Read Button
                PrimaryActionButton(
                    label = "Read",
                    onClick = onReadClick
                )
            }
        }
    }
}

// --- Component: Wide Standard Action Button ---
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    customIcon: @Composable (() -> Unit)? = null
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (customIcon != null) {
                customIcon()
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Component: Wide Primary Action Button ---
@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}