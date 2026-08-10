package com.yugentech.quill.ui.shared.bookDetails.components

import android.text.StaticLayout
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
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.text = spanned
            textView.maxLines = maxLines
            textView.ellipsize = TextUtils.TruncateAt.END
            textView.setTextColor(textColor.toArgb())
            textView.movementMethod = if (maxLines == Int.MAX_VALUE) {
                LinkMovementMethod.getInstance()
            } else {
                null
            }

            textView.post {
                val width = textView.width
                val fullLineCount = if (width > 0) {
                    StaticLayout.Builder
                        .obtain(spanned, 0, spanned.length, textView.paint, width)
                        .build()
                        .lineCount
                } else {
                    textView.lineCount
                }
                onLineCountChanged(fullLineCount)
            }
        }
    )
}