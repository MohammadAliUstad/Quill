package com.yugentech.quill.ui.about.aira.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.theme.WindSongFont

/**
 * The close of Meet Aira: a short note in Aira's own voice rather than a pitch. A large quote
 * mark, a few plain sentences on what she's there for, and her name signed at the bottom right
 * in the same script as the hero's.
 */
@Composable
fun AiraNote(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp)
    ) {
        // A large opening quote, set in a serif so it reads as a letter's mark, not an icon
        Text(
            text = "“",
            fontFamily = FontFamily.Serif,
            fontSize = 72.sp,
            lineHeight = 72.sp,
            fontWeight = FontWeight.Bold,
            color = primary.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = (-4).dp, y = 8.dp)
        )

        Text(
            text = stringResource(R.string.aira_note_body),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Signed off at the bottom right, her name in script, as a letter ends
        Text(
            text = "Aira",
            fontFamily = WindSongFont,
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        )
    }
}
