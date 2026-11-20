package com.yugentech.quill.ui.info.insights.components

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
import com.yugentech.quill.localization.currentLocale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@Composable
fun PeakHourCard(
    peakHour: Int?,
    modifier: Modifier = Modifier
) {
    val (icon, descriptorRes) = when (peakHour) {
        null -> Icons.Outlined.BedtimeOff to R.string.peak_none_body
        in 5..11 -> Icons.Outlined.LightMode to R.string.peak_morning
        in 12..16 -> Icons.Outlined.WbTwilight to R.string.peak_afternoon
        in 17..20 -> Icons.Outlined.WbTwilight to R.string.peak_evening
        else -> Icons.Outlined.ModeNight to R.string.peak_night
    }
    val locale = currentLocale()
    val timeLabel = if (peakHour == null) stringResource(R.string.peak_none_title) else formatHour(peakHour, locale)
    val descriptor = stringResource(descriptorRes)

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

/** The hour on the clock, in the language's own way of writing it ("7:00 PM", "19:00", "下午7:00"). */
private fun formatHour(hour: Int, locale: Locale): String =
    LocalTime.of(hour, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))