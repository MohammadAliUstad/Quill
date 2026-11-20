package com.yugentech.quill.ui.sources.standard.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.ui.tabs.discoverScreen.util.thumbnailCoverUrl
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import com.yugentech.quill.ui.tabs.discoverScreen.components.loadingPulse

@Composable
fun StandardBookItem(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    val isLoaded = imageState is AsyncImagePainter.State.Success
    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "imageAlpha"
    )
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (isLoaded) 0f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "placeholderAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onClick()
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The pulsing placeholder until the cover arrives, then it fades away under it
            if (placeholderAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(placeholderAlpha)
                        .loadingPulse()
                )
            }
            AsyncImage(
                model = book.thumbnailCoverUrl,
                contentDescription = stringResource(R.string.cover_of, book.title),
                contentScale = ContentScale.Crop,
                onState = { imageState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha)
            )
        }
    }
}