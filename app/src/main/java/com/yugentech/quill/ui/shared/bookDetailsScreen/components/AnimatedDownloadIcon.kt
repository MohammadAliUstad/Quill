package com.yugentech.quill.ui.shared.bookDetailsScreen.components

import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

@Composable
fun AnimatedDownloadIcon(
    drawableResId: Int,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current.toArgb()

    key(drawableResId) {
        AndroidView(
            // INTERNAL CANVAS: Set to 100.dp to match HeartIcon consistency
            modifier = modifier.size(100.dp),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
                    val drawable = AnimatedVectorDrawableCompat.create(ctx, drawableResId)
                    setImageDrawable(drawable)
                    (drawable as? Animatable)?.start()
                }
            },
            update = { imageView ->
                imageView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
                val drawable = imageView.drawable as? Animatable
                if (drawable?.isRunning == false) drawable.start()
            }
        )
    }
}