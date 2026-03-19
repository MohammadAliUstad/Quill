package com.yugentech.quill.ui.sources.common

import android.graphics.PorterDuff
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yugentech.quill.R

@Composable
fun AnimatedSearchIcon(
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current.toArgb()

    AndroidView(
        modifier = modifier.size(32.dp),
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(R.drawable.asl_searchback)
                isActivated = isSearchActive
                setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            }
        },
        update = { imageView ->
            imageView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN)
            imageView.isActivated = isSearchActive
        }
    )
}