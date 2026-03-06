package com.yugentech.quill.ui.shared.bookDetailsScreen.components

import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textColor: Color = Color.Black,
    onLineCountChanged: (Int) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 15f
                setLineSpacing(4f, 1.4f)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.maxLines = Int.MAX_VALUE // Always measure full height first
            textView.ellipsize = TextUtils.TruncateAt.END
            textView.setTextColor(textColor.toArgb())

            textView.movementMethod = if (maxLines == Int.MAX_VALUE) {
                LinkMovementMethod.getInstance()
            } else {
                null
            }

            // Get the actual full line count
            textView.post {
                val fullLineCount = textView.lineCount
                onLineCountChanged(fullLineCount)

                // Now apply the maxLines constraint after we've measured
                textView.maxLines = maxLines
            }
        }
    )
}