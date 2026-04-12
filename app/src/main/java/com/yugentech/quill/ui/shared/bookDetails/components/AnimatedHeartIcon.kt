package com.yugentech.quill.ui.shared.bookDetails.components

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.widget.ImageView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.yugentech.quill.R

@Composable
fun AnimatedHeartIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary.toArgb()
    val inactiveColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.asl_heart)
                this.isActivated = isLiked
                applyColorFilter(if (isLiked) activeColor else inactiveColor)
            }
        },
        update = { imageView ->
            val color = if (isLiked) activeColor else inactiveColor
            imageView.drawable?.colorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BlendModeColorFilter(color, BlendMode.SRC_IN)
            } else {
                @Suppress("DEPRECATION")
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            if (imageView.isActivated != isLiked) {
                imageView.isActivated = isLiked
                imageView.drawable?.colorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    BlendModeColorFilter(color, BlendMode.SRC_IN)
                } else {
                    @Suppress("DEPRECATION")
                    PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
            }
        }
    )
}

private fun ImageView.applyColorFilter(color: Int) {
    drawable?.colorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        @Suppress("DEPRECATION")
        PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}