package com.yugentech.quill.ui.info.indexing.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.loadingPulse
import com.yugentech.quill.ui.info.indexing.state.IndexingUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QueueGridItem(
    modifier: Modifier= Modifier,
    item: IndexingUiState
) {
    val coverShape = RoundedCornerShape(8.dp)

    // The cover fades in over a pulsing placeholder once it has loaded, as in the Library
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val isLoaded = imageState is AsyncImagePainter.State.Success
    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(500),
        label = "queueCoverAlpha"
    )
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 0f else 1f,
        animationSpec = tween(500),
        label = "queuePlaceholderAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = item.progress / 100f,
        animationSpec = tween(durationMillis = 500),
        label = "progressAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(2f / 3f)
            .clip(coverShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(placeholderAlpha)
                .loadingPulse()
        )

        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (item.isRunning) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = "${item.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Pending,
                        contentDescription = stringResource(R.string.waiting),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.waiting),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
