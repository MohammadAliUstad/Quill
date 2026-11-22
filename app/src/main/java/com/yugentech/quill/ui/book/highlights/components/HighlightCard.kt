package com.yugentech.quill.ui.book.highlights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.localization.currentLocale
import java.text.DateFormat
import java.util.Date
import org.json.JSONObject

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    highlight: HighlightEntity,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val unreadable = stringResource(R.string.highlight_unreadable)
    val unknownChapter = stringResource(R.string.unknown_chapter)
    val pageFormat = stringResource(R.string.chapter_page)
    val parseError = stringResource(R.string.highlight_parse_error)
    val unknownLocation = stringResource(R.string.unknown_location)
    val parsedData = remember(highlight.locatorJson, pageFormat) {
        try {
            val json = JSONObject(highlight.locatorJson)
            val text = json.optJSONObject("text")?.optString("highlight") ?: unreadable

            val chapter = json.optString("title").let { it.ifBlank { unknownChapter } }
            val position = json.optJSONObject("locations")?.optInt("position", -1) ?: -1
            val location = if (position > 0) pageFormat.format(chapter, position) else chapter

            text to location
        } catch (e: Exception) {
            parseError to unknownLocation
        }
    }

    val highlightText = parsedData.first
    val locationInfo = parsedData.second

    val locale = currentLocale()
    val dateString = remember(highlight.createdAt, locale) {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        formatter.format(Date(highlight.createdAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
            onCardClick()
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(IntrinsicSize.Min)
                    .background(Color(highlight.colorInt))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = locationInfo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "\"$highlightText\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontStyle = FontStyle.Italic
                )

                if (!highlight.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = highlight.note!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onDeleteClick()
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_highlight_cd),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
