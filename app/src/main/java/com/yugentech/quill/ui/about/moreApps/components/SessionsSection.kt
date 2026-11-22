package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.CapabilityItem

@Composable
fun SessionsHeroSection(onDownloadClick: () -> Unit) {
    AppHeroSection(
        name = "Sessions",
        tagline = stringResource(R.string.sessions_tagline),
        iconBackground = SessionsIconBackground,
        animationMillis = 900,
        onDownloadClick = onDownloadClick,
        icon = { isAnimating ->
            AnimatedSessionsIcon(
                isAnimating = isAnimating,
                modifier = Modifier.requiredSize(156.dp)
            )
        },
        footer = {
            // Play Store-style stats strip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoreStat(
                    value = "4.9",
                    showStar = true,
                    label = "100+ reviews",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                StoreStat(
                    value = "5K+",
                    showStar = false,
                    label = "Downloads",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
fun SessionsFeatures() {
    FeaturesSection(
        // Third carousel on the screen: tertiary, primary, secondary.
        colorOffset = 2,
        items = listOf(
            CapabilityItem(Icons.Outlined.Timer, stringResource(R.string.sessions_f1_title), stringResource(R.string.sessions_f1_body), slot = 0),
            CapabilityItem(Icons.Outlined.Headphones, stringResource(R.string.sessions_f2_title), stringResource(R.string.sessions_f2_body), slot = 1),
            CapabilityItem(Icons.Outlined.Analytics, stringResource(R.string.sessions_f3_title), stringResource(R.string.sessions_f3_body), slot = 2),
            CapabilityItem(Icons.Outlined.Security, stringResource(R.string.sessions_f4_title), stringResource(R.string.sessions_f4_body), slot = 0),
            CapabilityItem(Icons.Outlined.ColorLens, stringResource(R.string.sessions_f5_title), stringResource(R.string.sessions_f5_body), slot = 1),
            CapabilityItem(Icons.Outlined.History, stringResource(R.string.sessions_f6_title), stringResource(R.string.sessions_f6_body), slot = 2)
        )
    )
}
