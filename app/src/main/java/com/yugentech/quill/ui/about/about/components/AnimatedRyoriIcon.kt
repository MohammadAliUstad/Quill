package com.yugentech.quill.ui.about.about.components

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.yugentech.quill.R

// Same as AnimatedSessionsIcon, for Ryori's burger. It rests on the fully assembled burger and
// replays the build-up when isAnimating becomes true (the AVD runs for about 1200ms).
@Composable
fun AnimatedRyoriIcon(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.avd_ryori_burger)
            }
        },
        update = { imageView ->
            if (isAnimating) {
                (imageView.drawable as? Animatable)?.let {
                    it.stop()
                    it.start()
                }
            }
        }
    )
}
