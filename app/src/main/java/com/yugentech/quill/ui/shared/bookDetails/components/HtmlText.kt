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
            // movementMethod must be set before text is assigned: TextView decides
            // whether it can ellipsize based on the movementMethod in place at the
            // time checkForRelayout() runs inside setText(). Setting it after leaves
            // a stale layout that never re-enables the "..." once a link movement
            // method has ever been attached (from a prior expanded state).
            textView.maxLines = maxLines
            textView.ellipsize = TextUtils.TruncateAt.END
            textView.setTextColor(textColor.toArgb())
            textView.movementMethod = if (maxLines == Int.MAX_VALUE) {
                LinkMovementMethod.getInstance()
            } else {
                null
            }
            textView.text = spanned

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