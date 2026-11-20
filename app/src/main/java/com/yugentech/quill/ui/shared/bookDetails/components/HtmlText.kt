package com.yugentech.quill.ui.shared.bookDetails.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * A book's description, from its HTML (Standard Ebooks writes paragraphs; Gutenberg summaries are
 * plain text). Drawn by Compose, with nothing around the text but the text itself: the blank lines
 * HTML paragraphs leave at the end are trimmed, and the line height adds no space above the first
 * line or below the last. So what's around it (the expand arrow, the subjects) sits exactly where
 * the layout puts it, for every book alike.
 *
 * [onLineCountChanged] reports how many lines the full text takes, or at least one more than shown
 * while it's cut short, so the caller knows whether there's more to expand.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textColor: Color = Color.Black,
    onLineCountChanged: (Int) -> Unit = {}
) {
    val text = remember(html) { AnnotatedString.fromHtml(html).trimmed() }

    Text(
        text = text,
        modifier = modifier,
        color = textColor,
        // The app's font, from the theme's body style, at the description's own size and spacing
        style = MaterialTheme.typography.bodyLarge.merge(DescriptionStyle),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            onLineCountChanged(if (layout.hasVisualOverflow) layout.lineCount + 1 else layout.lineCount)
        }
    )
}

// The same size and airy spacing the description had as a TextView (15sp, 1.4x lines).
// Trim.Both: the extra line height goes between lines, never at the ends.
private val DescriptionStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 25.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

/** Without the whitespace (paragraph breaks, mostly) at either end, keeping its styling. */
private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return subSequence(start, end)
}
