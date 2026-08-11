package com.yugentech.quill.ui.about.about.components

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.yugentech.quill.R

@Composable
fun AnimatedQuillIcon(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.avd_quill)
            }
        },
        update = { imageView ->
            if (isAnimating) {
                (imageView.drawable as? Animatable)?.start()
            }
        }
    )
}
