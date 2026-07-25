package com.yugentech.quill.ui.shared.bookDetails.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.yugentech.quill.R
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookHeaderContent(
    book: Book,
    topPadding: Dp,
    onCategoryClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onReadClick: () -> Unit
) {
    var wasDownloading by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(book.downloadStatus) {
        if (book.downloadStatus == DownloadStatus.DOWNLOADING) {
            wasDownloading = true
        }
    }

    val isSaved = book.userCategory != null
    val isDownloaded = book.downloadStatus == DownloadStatus.DOWNLOADED
    val isDownloadEnabled = book.downloadStatus != DownloadStatus.DOWNLOADING && !isDownloaded

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 16.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ElevatedCard(
            onClick = { showCoverDialog = true },
            modifier = Modifier
                .width(160.dp)
                .height(240.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
        ) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Book Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .height(240.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Metadata
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmartMarqueeTitle(title = book.title)
                Text(
                    modifier = Modifier.basicMarquee(),
                    text = book.author,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                if (book.downloadStatus == DownloadStatus.FAILED && book.downloadError != null) {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = book.downloadError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val categoryLabel = book.userCategory ?: "Add to Library"
                val categoryIcon = if (isSaved) Icons.Default.FolderOpen else Icons.Default.LibraryAdd

                ActionButton(
                    icon = categoryIcon,
                    label = categoryLabel,
                    onClick = onCategoryClick,
                    isTop = true
                )

                Spacer(modifier = Modifier.height(2.dp))

                ActionButton(
                    icon = if (book.downloadStatus == DownloadStatus.FAILED) Icons.Default.Refresh else Icons.Default.Download,
                    label = when (book.downloadStatus) {
                        DownloadStatus.DOWNLOADED -> "Downloaded"
                        DownloadStatus.DOWNLOADING -> "Downloading"
                        DownloadStatus.FAILED -> "Retry Download"
                        else -> "Download"
                    },
                    onClick = { if (isDownloadEnabled || book.downloadStatus == DownloadStatus.FAILED) onDownloadClick() },
                    isTop = false,
                    customIcon = {
                        when (book.downloadStatus) {
                            DownloadStatus.DOWNLOADING -> {
                                AnimatedDownloadIcon(
                                    drawableResId = R.drawable.downloading_start,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            DownloadStatus.DOWNLOADED -> {
                                if (wasDownloading) {
                                    AnimatedDownloadIcon(
                                        drawableResId = R.drawable.download_complete,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.download_mark),
                                        contentDescription = "Downloaded",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            else -> null
                        }
                    }
                )

                AnimatedVisibility(
                    visible = isDownloaded,
                    enter = if (wasDownloading) {
                        expandVertically(expandFrom = Alignment.Top) + fadeIn()
                    } else {
                        EnterTransition.None
                    },
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(2.dp))
                        DetachedReadButton(book = book, onClick = onReadClick)
                    }
                }
            }
        }
    }

    if (showCoverDialog) {
        CoverImageDialog(
            coverUrl = book.coverUrl,
            onDismiss = { showCoverDialog = false }
        )
    }
}

@Composable
fun CoverImageDialog(
    coverUrl: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Full Book Cover",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }
    }
}

@Composable
fun SmartMarqueeTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    var firstLine by remember(title) { mutableStateOf(title) }
    var secondLine by remember(title) { mutableStateOf<String?>(null) }
    var isMeasured by remember(title) { mutableStateOf(false) }

    val visibleStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.25f),
            blurRadius = 4f
        )
    )

    val layoutStyle = visibleStyle.copy(shadow = null)

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = layoutStyle,
            maxLines = 2,
            color = Color.Transparent,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.lineCount > 1) {
                    val splitIndex = textLayoutResult.getLineEnd(0, visibleEnd = true)

                    if (splitIndex < title.length) {
                        firstLine = title.substring(0, splitIndex).trim()
                        secondLine = title.substring(splitIndex).trim()
                    } else {
                        firstLine = title
                        secondLine = null
                    }
                } else {
                    firstLine = title
                    secondLine = null
                }
                isMeasured = true
            }
        )

        if (isMeasured) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = firstLine,
                    style = visibleStyle,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onBackground,
                    overflow = TextOverflow.Clip
                )

                if (secondLine != null) {
                    Text(
                        text = secondLine!!,
                        style = visibleStyle,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isTop: Boolean,
    customIcon: @Composable (() -> Unit)? = null
) {
    val shape = if (isTop) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val containerColor = if (isTop) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    }

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (customIcon != null) {
                customIcon()
            } else {
                Icon(icon, null, Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DetachedReadButton(
    book: Book,
    onClick: () -> Unit
) {
    val buttonText = remember(book.progressPercent) {
        if (book.progressPercent > 0f) "Continue" else "Read"
    }

    Card(
        onClick = onClick,
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
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
                text = buttonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
