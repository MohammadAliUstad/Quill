package com.yugentech.quill.ui.sources.standard.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The source's server is down for maintenance: says so, with when it should be back (in the
 * reader's own time zone and language) if the server said, so a blank or failing screen isn't
 * mistaken for a fault in the app.
 */
@Composable
fun SourceMaintenanceState(
    backAfter: Instant?,
    modifier: Modifier = Modifier
) {
    val backAt = rememberBackAfterText(backAfter)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Construction,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.source_maintenance_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (backAt != null) stringResource(R.string.source_maintenance_body_until, backAt)
            else stringResource(R.string.source_maintenance_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * When a source down for maintenance should be back, as the reader would say it: their language,
 * their time zone ("Thursday, 19 November, 2:30 am"). Null when it didn't say, or the time's past.
 */
@Composable
fun rememberBackAfterText(backAfter: Instant?): String? {
    val locale = LocalConfiguration.current.locales[0]
    return remember(backAfter, locale) {
        backAfter?.takeIf { it.isAfter(Instant.now()) }?.let {
            val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMjmm")
            DateTimeFormatter.ofPattern(pattern, locale).format(it.atZone(ZoneId.systemDefault()))
        }
    }
}
