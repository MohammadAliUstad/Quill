package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components

import android.graphics.PorterDuff
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yugentech.quill.R

@Composable
fun AnimatedHeartIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Define colors
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val inactiveColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val currentColor = if (isLiked) activeColor else inactiveColor

    AndroidView(
        modifier = modifier.size(72.dp),
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.asl_heart)

                // Initial State
                isActivated = isLiked
                setColorFilter(currentColor, PorterDuff.Mode.SRC_IN)

                // REMOVED: setOnClickListener.
                // We let the parent Composable (IconButton) handle the clicks.
            }
        },
        update = { imageView ->
            imageView.setColorFilter(currentColor, PorterDuff.Mode.SRC_IN)

            if (imageView.isActivated != isLiked) {
                imageView.isActivated = isLiked
                // This state change automatically triggers the XML animation
            }
        }
    )
}