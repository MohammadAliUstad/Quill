package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.CapabilityItem

@Composable
fun RyoriHeroSection() {
    AppHeroSection(
        name = "Ryori",
        tagline = stringResource(R.string.ryori_tagline),
        iconBackground = RyoriIconBackground,
        animationMillis = 1200,
        onDownloadClick = null,
        icon = { isAnimating ->
            // Fills the tile: the drawable has a margin round the burger (still 64dp), so the
            // buns slide in from the tile's edges.
            AnimatedRyoriIcon(
                isAnimating = isAnimating,
                modifier = Modifier.requiredSize(88.dp)
            )
        }
    )
}

@Composable
fun RyoriFeatures() {
    FeaturesSection(
        // Fourth carousel on the screen: back to primary, secondary, tertiary.
        colorOffset = 0,
        items = listOf(
            CapabilityItem(Icons.Outlined.Home, stringResource(R.string.ryori_f1_title), stringResource(R.string.ryori_f1_body), slot = 0),
            CapabilityItem(Icons.Outlined.TravelExplore, stringResource(R.string.ryori_f2_title), stringResource(R.string.ryori_f2_body), slot = 1),
            CapabilityItem(Icons.Outlined.Checklist, stringResource(R.string.ryori_f3_title), stringResource(R.string.ryori_f3_body), slot = 2),
            CapabilityItem(Icons.Outlined.LocalBar, stringResource(R.string.ryori_f4_title), stringResource(R.string.ryori_f4_body), slot = 0),
            CapabilityItem(Icons.Outlined.Eco, stringResource(R.string.ryori_f5_title), stringResource(R.string.ryori_f5_body), slot = 1),
            CapabilityItem(Icons.Outlined.CloudOff, stringResource(R.string.ryori_f6_title), stringResource(R.string.ryori_f6_body), slot = 2)
        )
    )
}
