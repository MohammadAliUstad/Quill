package com.yugentech.quill.ui.more.insightsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.yugentech.theme.tokens.icons
import com.yugentech.theme.tokens.spacing

@Composable
fun PeakHourCard(
    peakHour: Int?,
    modifier: Modifier = Modifier
) {
    val (icon, timeLabel, descriptor) = remember(peakHour) {
        when (peakHour) {
            null -> Triple(
                Icons.Outlined.BedtimeOff,
                "No data yet",
                "Start reading to discover your rhythm"
            )
            in 5..11 -> Triple(
                Icons.Outlined.LightMode,
                formatHour(peakHour),
                "You're a morning reader"
            )
            in 12..16 -> Triple(
                Icons.Outlined.WbTwilight,
                formatHour(peakHour),
                "Afternoons are your reading time"
            )
            in 17..20 -> Triple(
                Icons.Outlined.WbTwilight,
                formatHour(peakHour),
                "Evenings are when you settle in"
            )
            else -> Triple(
                Icons.Outlined.ModeNight,
                formatHour(peakHour),
                "A night owl reader"
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.icons.large)
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = descriptor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatHour(hour: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 $suffix"
}