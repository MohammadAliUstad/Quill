package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components

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
                setImageResource(com.yugentech.quill.R.drawable.asl_heart)
                // Set initial state immediately — no post{} — so the drawable
                // starts in the correct resting state before update() runs.
                // Using isActivated as the single source of truth: it feeds into
                // the view's draw state array, which AnimatedStateListDrawable
                // reads via onStateChange to pick the right item and transition.
                // We never call drawable.setState() directly — that bypasses the
                // view's state machinery and causes the two to desync.
                this.isActivated = isLiked
                applyColorFilter(if (isLiked) activeColor else inactiveColor)
            }
        },
        update = { imageView ->
            val color = if (isLiked) activeColor else inactiveColor
            // Apply color filter to the ImageView's drawable via the drawable's
            // own colorFilter property instead of ImageView.setColorFilter().
            // This persists across drawable swaps that AnimatedStateListDrawable
            // performs during transitions, so the animated vector frames stay tinted.
            imageView.drawable?.colorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BlendModeColorFilter(color, BlendMode.SRC_IN)
            } else {
                @Suppress("DEPRECATION")
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            // isActivated is the single mechanism driving state — setting it here
            // is the only state call we make. AnimatedStateListDrawable observes
            // the view's state array change and fires the correct transition.
            if (imageView.isActivated != isLiked) {
                imageView.isActivated = isLiked
                // Re-apply color filter immediately after state change because
                // the transition swaps in a new drawable at this moment.
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