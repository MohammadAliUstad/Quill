package com.yugentech.quill.ui.tabs.discoverScreen.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.Book

@Composable
fun DiscoverBookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val isLoaded = imageState is AsyncImagePainter.State.Success
    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(400),
        label = "cardImageAlpha"
    )
    val shimmerAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 0f else 1f,
        animationSpec = tween(400),
        label = "cardShimmerAlpha"
    )

    val coverShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .width(130.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(coverShape)
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(shimmerAlpha)
                    .shimmerEffect()
            )

            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                onState = { imageState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = imageAlpha
                        shadowElevation = 6.dp.toPx()
                        shape = coverShape
                        clip = true
                    }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}