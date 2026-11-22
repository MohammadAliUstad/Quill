package com.yugentech.quill.ui.book.bookDetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.ui.common.components.rememberBackAfterText
import java.time.Instant

/**
 * In place of the description while the book's source is down for maintenance and the book isn't
 * downloaded: why it can't be downloaded, and when it should be back if the source said. Laid out
 * like Discover's maintenance card: an icon beside a title and a line of text.
 */
@Composable
fun SourceMaintenanceSection(
    source: BookSource,
    backAfter: Instant?,
    modifier: Modifier = Modifier
) {
    val sourceName = when (source) {
        BookSource.STANDARD_EBOOKS -> stringResource(R.string.source_standard_name)
        BookSource.USER_IMPORTED -> stringResource(R.string.download_maintenance_this_source)
    }
    val backAt = rememberBackAfterText(backAfter)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Space above only: it's the page's last section, and the page adds its own 8dp below
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.download_maintenance_title, sourceName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = if (backAt != null) stringResource(R.string.download_maintenance_body_until, backAt)
                else stringResource(R.string.download_maintenance_body),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
