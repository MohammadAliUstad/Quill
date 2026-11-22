package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.CapabilityItem

@Composable
fun HajimeHeroSection() {
    AppHeroSection(
        name = "Hajime",
        tagline = stringResource(R.string.hajime_tagline),
        iconBackground = HajimeIconBackground,
        animationMillis = 1660,
        onDownloadClick = null,
        icon = { isAnimating ->
            AnimatedHajimeIcon(
                isAnimating = isAnimating,
                modifier = Modifier.requiredSize(156.dp)
            )
        }
    )
}

@Composable
fun HajimeFeatures() {
    FeaturesSection(
        // Second carousel on the screen: secondary, tertiary, primary.
        colorOffset = 1,
        items = listOf(
            CapabilityItem(Icons.Outlined.Translate, stringResource(R.string.hajime_tagline), stringResource(R.string.hajime_f1_body), slot = 0),
            CapabilityItem(Icons.Outlined.School, stringResource(R.string.hajime_f2_title), stringResource(R.string.hajime_f2_body), slot = 1),
            CapabilityItem(Icons.Outlined.History, stringResource(R.string.hajime_f3_title), stringResource(R.string.hajime_f3_body), slot = 2),
            CapabilityItem(Icons.Outlined.AutoAwesome, stringResource(R.string.hajime_f4_title), stringResource(R.string.hajime_f4_body), slot = 0),
            CapabilityItem(Icons.Outlined.RecordVoiceOver, stringResource(R.string.hajime_f5_title), stringResource(R.string.hajime_f5_body), slot = 1),
            CapabilityItem(Icons.Outlined.Insights, stringResource(R.string.hajime_f6_title), stringResource(R.string.hajime_f6_body), slot = 2)
        )
    )
}
