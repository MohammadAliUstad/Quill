package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.Book

import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject

@Composable
fun HeroCarouselCard(
    book: Book,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = koinInject<HapticService>()
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val isLoaded = imageState is AsyncImagePainter.State.Success

    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(600),
        label = "heroImageAlpha"
    )
    val shimmerAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 0f else 1f,
        animationSpec = tween(600),
        label = "heroShimmerAlpha"
    )

    val coverShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(coverShape)
    ) {
        // Shimmer placeholder — visible until image loads
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(shimmerAlpha)
                .shimmerEffect()
        )

        // Actual image — fades in on top
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    haptic.performHaptic()
                    onBookClick(book)
                }
                .graphicsLayer {
                    alpha = imageAlpha
                    shadowElevation = 16.dp.toPx()
                    shape = coverShape
                    clip = true
                }
        )
    }
}