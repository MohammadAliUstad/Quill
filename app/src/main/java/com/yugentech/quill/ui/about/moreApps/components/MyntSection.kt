package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.CapabilityItem

@Composable
fun MyntHeroSection() {
    AppHeroSection(
        name = "Mynt",
        tagline = stringResource(R.string.mynt_tagline),
        iconBackground = MyntIconBackground,
        animationMillis = 1300,
        // Not on the Play Store yet
        onDownloadClick = null,
        icon = { isAnimating ->
            // Drawn larger than the tile, as Mynt itself does, so the wallet fills it and the
            // tile clips the safe-zone margin.
            AnimatedMyntIcon(
                isAnimating = isAnimating,
                modifier = Modifier.requiredSize(132.dp)
            )
        }
    )
}

/** Mynt's own key features, from its Meet Mynt screen. */
@Composable
fun MyntFeatures() {
    FeaturesSection(
        items = listOf(
            CapabilityItem(Icons.Outlined.KeyboardVoice, stringResource(R.string.mynt_f1_title), stringResource(R.string.mynt_f1_body), slot = 0),
            CapabilityItem(Icons.Outlined.Handshake, stringResource(R.string.mynt_f2_title), stringResource(R.string.mynt_f2_body), slot = 1),
            CapabilityItem(Icons.AutoMirrored.Outlined.CallSplit, stringResource(R.string.mynt_f3_title), stringResource(R.string.mynt_f3_body), slot = 2),
            CapabilityItem(Icons.Outlined.QueryStats, stringResource(R.string.mynt_f4_title), stringResource(R.string.mynt_f4_body), slot = 0),
            CapabilityItem(Icons.Outlined.EditNote, stringResource(R.string.mynt_f5_title), stringResource(R.string.mynt_f5_body), slot = 1),
            CapabilityItem(Icons.Outlined.RecordVoiceOver, stringResource(R.string.mynt_f6_title), stringResource(R.string.mynt_f6_body), slot = 2)
        )
    )
}
