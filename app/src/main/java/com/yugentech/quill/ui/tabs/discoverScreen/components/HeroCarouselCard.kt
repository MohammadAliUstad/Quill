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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.Book

@Composable
fun HeroCarouselCard(
    book: Book,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageState is AsyncImagePainter.State.Success) 1f else 0f,
        animationSpec = tween(600),
        label = "heroImageAlpha"
    )

    val coverShape = RoundedCornerShape(12.dp)

    Box(
        // We completely removed the fixed height. It responds to the Row's constraints now.
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxWidth() // Fill the half-width provided by the Carousel Row
                .aspectRatio(2f / 3f) // Classic book cover proportions dynamically sets height
                .shadow(elevation = 16.dp, shape = coverShape)
                .clip(coverShape)
                .clickable { onBookClick(book) }
                .alpha(imageAlpha)
        )
    }
}