package com.yugentech.quill.ui.tabs.libraryScreen.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.ui.tabs.discoverScreen.components.shimmerEffect
import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookItem(
    modifier: Modifier = Modifier,
    book: LibraryBookView,
    needsTwoLines: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val isLoaded = imageState is AsyncImagePainter.State.Success
    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(500),
        label = "bookItemImageAlpha"
    )
    val shimmerAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 0f else 1f,
        animationSpec = tween(500),
        label = "bookItemShimmerAlpha"
    )

    Column(
        modifier = modifier
            .width(115.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                haptic.performHaptic()
                onClick()
            }
            .padding(2.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(shimmerAlpha)
                        .shimmerEffect()
                )

                // Image fades in
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { imageState = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(imageAlpha)
                )

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (book.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }

                    if (book.progressPercent in 0.0f..1.0f && book.progressPercent > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(book.progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                        }
                    }

                    if (book.progressPercent >= 1.0f) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(20.dp)
                        )
                    }
                }

                if (book.downloadStatus == DownloadStatus.DOWNLOADING) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else if (book.downloadStatus == DownloadStatus.FAILED) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            minLines = if (needsTwoLines) 2 else 1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}