package com.yugentech.quill.ui.shared.airaChat.components

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
fun AnimatedRadioButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.asl_checkable_radiobutton)
                this.isActivated = isSelected
                applyColorFilter(color)
            }
        },
        update = { imageView ->
            if (imageView.isActivated != isSelected) {
                imageView.isActivated = isSelected
            }
        }
    )
}

private fun ImageView.applyColorFilter(color: Int) {
    val filter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        @Suppress("DEPRECATION")
        PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
    drawable?.colorFilter = filter
}